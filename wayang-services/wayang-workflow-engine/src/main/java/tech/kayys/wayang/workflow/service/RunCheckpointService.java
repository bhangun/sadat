package tech.kayys.wayang.workflow.service;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.workflow.domain.Checkpoint;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.workflow.model.WorkflowExecutionState;
import tech.kayys.wayang.workflow.repository.CheckpointRepository;
import tech.kayys.wayang.workflow.exception.CheckpointNotFoundException;

import java.time.Instant;
import java.util.List;

/**
 * RunCheckpointService - Manages workflow execution checkpoints for recovery.
 * 
 * Purpose:
 * - Create execution snapshots at key points
 * - Enable recovery from last known good state
 * - Support replay and debugging
 * - Minimize data loss on failures
 * 
 * Checkpoint Strategy:
 * - Checkpoint after each node execution
 * - Checkpoint before/after HITL suspension
 * - Checkpoint on explicit request
 * - Keep last N checkpoints per run
 * 
 * Recovery Strategy:
 * - Restore from latest checkpoint
 * - Replay from checkpoint forward
 * - Handle idempotency for replay
 */
@ApplicationScoped
public class RunCheckpointService {

    @Inject
    CheckpointRepository checkpointRepository;

    private static final int MAX_CHECKPOINTS_PER_RUN = 10;

    /**
     * Create a checkpoint for the current run state
     */
    public Uni<Checkpoint> createCheckpoint(WorkflowRun run) {
        return Uni.createFrom().deferred(() -> {
            Log.debugf("Creating checkpoint for run: %s", run.getRunId());

            Checkpoint checkpoint = new Checkpoint();
            checkpoint.setCheckpointId(java.util.UUID.randomUUID().toString());
            checkpoint.setRunId(run.getRunId());
            checkpoint.setTenantId(run.getTenantId());
            checkpoint.setSequenceNumber(getNextSequenceNumber(run.getRunId()));
            checkpoint.setExecutionState(deepCopy(run.getExecutionState()));
            checkpoint.setStatus(run.getStatus().name());
            checkpoint.setPhase(run.getPhase() != null ? run.getPhase().name() : null);
            checkpoint.setCreatedAt(Instant.now());
            checkpoint.setNodesExecuted(run.getNodesExecuted());

            return checkpointRepository.persist(checkpoint)
                    .invoke(saved -> {
                        Log.debugf("Checkpoint created: %s for run: %s",
                                saved.getCheckpointId(), run.getRunId());

                        // Update run with checkpoint reference
                        run.getExecutionState().setLastCheckpointId(saved.getCheckpointId());
                        run.getExecutionState().setLastCheckpointAt(saved.getCreatedAt());
                    })
                    .call(() -> pruneOldCheckpoints(run.getRunId()));
        });
    }

    /**
     * Get the latest checkpoint for a run
     */
    public Uni<Checkpoint> getLatestCheckpoint(String runId) {
        return checkpointRepository.findLatestByRunId(runId);
    }

    /**
     * Get checkpoint by ID
     */
    public Uni<Checkpoint> getCheckpoint(String checkpointId) {
        return checkpointRepository.findById(checkpointId);
    }

    /**
     * List all checkpoints for a run
     */
    public Uni<List<Checkpoint>> listCheckpoints(String runId) {
        return checkpointRepository.findByRunId(runId);
    }

    /**
     * Restore run state from checkpoint
     */
    public Uni<WorkflowExecutionState> restoreFromCheckpoint(String checkpointId) {
        return checkpointRepository.findById(checkpointId)
                .onItem().ifNull().failWith(
                        () -> new CheckpointNotFoundException(
                                "Checkpoint not found: " + checkpointId))
                .map(checkpoint -> {
                    Log.infof("Restoring state from checkpoint: %s", checkpointId);
                    return deepCopy(checkpoint.getExecutionState());
                });
    }

    /**
     * Delete checkpoints for a run (cleanup)
     */
    public Uni<Void> deleteCheckpoints(String runId) {
        return checkpointRepository.deleteByRunId(runId)
                .invoke(() -> Log.debugf("Deleted checkpoints for run: %s", runId));
    }

    /**
     * Prune old checkpoints (keep only last N)
     */
    private Uni<Void> pruneOldCheckpoints(String runId) {
        return checkpointRepository.findByRunId(runId)
                .onItem().transformToUni(checkpoints -> {
                    if (checkpoints.size() <= MAX_CHECKPOINTS_PER_RUN) {
                        return Uni.createFrom().voidItem();
                    }

                    // Sort by sequence and keep last N
                    checkpoints.sort((a, b) -> Integer.compare(b.getSequenceNumber(), a.getSequenceNumber()));

                    List<String> idsToDelete = checkpoints.stream()
                            .skip(MAX_CHECKPOINTS_PER_RUN)
                            .map(Checkpoint::getCheckpointId)
                            .toList();

                    if (idsToDelete.isEmpty()) {
                        return Uni.createFrom().voidItem();
                    }

                    return checkpointRepository.deleteByIds(idsToDelete)
                            .invoke(() -> Log.debugf(
                                    "Pruned %d old checkpoints for run: %s",
                                    idsToDelete.size(), runId));
                });
    }

    /**
     * Get next sequence number for checkpoints
     */
    private int getNextSequenceNumber(String runId) {
        return checkpointRepository.getMaxSequence(runId)
                .await().indefinitely()
                .map(max -> max + 1)
                .orElse(1);
    }

    /**
     * Deep copy of execution state (to prevent mutation)
     */
    private WorkflowExecutionState deepCopy(WorkflowExecutionState state) {
        if (state == null) {
            return null;
        }

        // Use serialization for deep copy
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = mapper.writeValueAsString(state);
            return mapper.readValue(json, WorkflowExecutionState.class);
        } catch (Exception e) {
            Log.error("Failed to deep copy execution state", e);
            throw new RuntimeException("Checkpoint serialization failed", e);
        }
    }
}
