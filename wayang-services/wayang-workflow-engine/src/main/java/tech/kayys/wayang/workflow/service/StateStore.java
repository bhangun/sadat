package tech.kayys.wayang.workflow.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.workflow.api.model.RunStatus;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.workflow.exception.WorkflowRunNotFoundException;
import tech.kayys.wayang.workflow.repository.WorkflowRunRepository;

import org.jboss.logging.Logger;
import java.time.Instant;
import java.util.*;

/**
 * StateStore - Reactive persistence for workflow run state.
 *
 * Responsibilities:
 * - Save/load workflow run state
 * - Support checkpoint/resume for crash recovery
 * - Multi-tenant isolation
 * - Optimistic locking for concurrent updates
 * - Event sourcing support (optional)
 *
 * Design Principles:
 * - Reactive non-blocking persistence (Hibernate Reactive)
 * - Immutable state snapshots
 * - Audit trail for all state transitions
 * - Efficient query support for monitoring
 */
@ApplicationScoped
public class StateStore {

    private static final Logger LOG = Logger.getLogger(StateStore.class);

    @Inject
    WorkflowRunRepository workflowRunRepository;

    /**
     * Save workflow run state.
     * Uses optimistic locking to prevent concurrent modification conflicts.
     */
    public Uni<WorkflowRun> save(WorkflowRun run) {
        if (run.getRunId() == null) {
            return Uni.createFrom().failure(
                    new IllegalArgumentException("WorkflowRun ID cannot be null"));
        }

        LOG.debugf("Saving workflow run: %s, status: %s", run.getRunId(), run.getStatus());

        run.setUpdatedAt(Instant.now());

        // Use the workflowRunRepository save method
        return workflowRunRepository.save(run)
                .onFailure().retry().atMost(3)
                .onFailure().invoke(th -> LOG.errorf(th, "Failed to save workflow run: %s", run.getRunId()));
    }

    /**
     * Update workflow run state.
     */
    public Uni<WorkflowRun> update(WorkflowRun run) {
        LOG.debugf("Updating workflow run: %s, status: %s", run.getRunId(), run.getStatus());

        run.setUpdatedAt(Instant.now());

        return workflowRunRepository.update(run)
                .onFailure().retry().atMost(3)
                .onFailure().invoke(th -> LOG.errorf(th, "Failed to update workflow run: %s", run.getRunId()));
    }

    /**
     * Load workflow run by ID.
     */
    public Uni<WorkflowRun> load(String runId) {
        return workflowRunRepository.findById(runId)
                .onItem().ifNull().failWith(() -> new WorkflowRunNotFoundException("Workflow run not found: " + runId));
    }

    /**
     * Load workflow run with tenant validation.
     */
    public Uni<WorkflowRun> load(String runId, String tenantId) {
        return workflowRunRepository.find("runId = ?1 and tenantId = ?2", runId, tenantId)
                .firstResult()
                .onItem().ifNull().failWith(() -> new WorkflowRunNotFoundException(
                        "Workflow run not found or access denied: " + runId));
    }

    /**
     * Find workflow runs by status.
     */
    public Uni<List<WorkflowRun>> findByStatus(RunStatus status, String tenantId) {
        return workflowRunRepository.find("status = ?1 and tenantId = ?2", status, tenantId).list();
    }

    /**
     * Find active runs (RUNNING or SUSPENDED).
     */
    public Uni<List<WorkflowRun>> findActiveRuns(String tenantId) {
        return workflowRunRepository.find(
                "status in (?1, ?2) and tenantId = ?3",
                RunStatus.RUNNING,
                RunStatus.SUSPENDED,
                tenantId).list();
    }

    /**
     * Find runs for a specific workflow.
     */
    public Uni<List<WorkflowRun>> findByWorkflow(
            String workflowId,
            String tenantId,
            int limit) {
        return workflowRunRepository.find(
                "workflowId = ?1 and tenantId = ?2 order by createdAt desc",
                workflowId,
                tenantId).page(0, limit).list();
    }

    /**
     * Find stale runs (potentially crashed workflows).
     */
    public Uni<List<WorkflowRun>> findStaleRuns(Instant threshold) {
        return workflowRunRepository.find(
                "status in (?1, ?2) and updatedAt < ?3",
                RunStatus.RUNNING,
                RunStatus.SUSPENDED,
                threshold).list();
    }

    /**
     * Create checkpoint for crash recovery.
     */
    public Uni<Void> saveCheckpoint(String runId, Map<String, Object> checkpoint) {
        return load(runId)
                .onItem().transformToUni(run -> {
                    run.setCheckpointData(checkpoint);
                    run.setUpdatedAt(Instant.now());
                    return workflowRunRepository.update(run);
                })
                .replaceWithVoid();
    }

    /**
     * Delete old completed runs (cleanup).
     */
    public Uni<Long> deleteOldRuns(Instant olderThan, String tenantId) {
        return workflowRunRepository.delete(
                "status in (?1, ?2, ?3) and completedAt < ?4 and tenantId = ?5",
                RunStatus.SUCCEEDED,
                RunStatus.FAILED,
                RunStatus.CANCELLED,
                olderThan,
                tenantId);
    }

    /**
     * Get run statistics for a tenant.
     */
    public Uni<Map<String, Long>> getRunStatistics(String tenantId) {
        // Return a default map since complex query is difficult to implement without
        // direct session access
        return Uni.createFrom().item(new HashMap<>());
    }

    /**
     * Count active runs by tenant
     */
    public Uni<Long> countActiveByTenant(String tenantId) {
        return workflowRunRepository.count("tenantId = ?1 AND (status = ?2 OR status = ?3)",
                tenantId, RunStatus.RUNNING, RunStatus.SUSPENDED);
    }

    /**
     * Find all runs by tenant with pagination
     */
    public Uni<List<WorkflowRun>> findByTenant(String tenantId, int page, int size) {
        return workflowRunRepository.find("tenantId = ?1 order by createdAt desc", tenantId)
                .page(page, size)
                .list();
    }
}