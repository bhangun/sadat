package tech.kayys.wayang.workflow.kernel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Snapshot of a workflow run at a specific point in time.
 * Used for debugging, recovery, and monitoring.
 */
@Data
@Builder(toBuilder = true)
public class WorkflowRunSnapshot {

    private final String snapshotId;
    private final WorkflowRunId runId;
    private final WorkflowRunState state;
    private final Instant snapshotTime;
    private final ExecutionContext context;
    private final List<NodeExecutionRecord> nodeHistory;
    private final Map<String, Object> workflowState;
    private final Map<String, Object> metadata;
    private final long eventCount;

    @Builder.Default
    private final boolean isCheckpoint = false;

    @Builder.Default
    private final String checkpointReason = null;

    @JsonCreator
    public WorkflowRunSnapshot(
            @JsonProperty("snapshotId") String snapshotId,
            @JsonProperty("runId") WorkflowRunId runId,
            @JsonProperty("state") WorkflowRunState state,
            @JsonProperty("snapshotTime") Instant snapshotTime,
            @JsonProperty("context") ExecutionContext context,
            @JsonProperty("nodeHistory") List<NodeExecutionRecord> nodeHistory,
            @JsonProperty("workflowState") Map<String, Object> workflowState,
            @JsonProperty("metadata") Map<String, Object> metadata,
            @JsonProperty("eventCount") long eventCount,
            @JsonProperty("isCheckpoint") boolean isCheckpoint,
            @JsonProperty("checkpointReason") String checkpointReason) {

        this.snapshotId = snapshotId != null ? snapshotId : java.util.UUID.randomUUID().toString();
        this.runId = runId;
        this.state = state;
        this.snapshotTime = snapshotTime != null ? snapshotTime : Instant.now();
        this.context = context;
        this.nodeHistory = nodeHistory != null ? List.copyOf(nodeHistory) : List.of();
        this.workflowState = workflowState != null ? Map.copyOf(workflowState) : Map.of();
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        this.eventCount = eventCount;
        this.isCheckpoint = isCheckpoint;
        this.checkpointReason = checkpointReason;
    }

    // Factory methods
    public static WorkflowRunSnapshot create(
            WorkflowRunId runId,
            WorkflowRunState state,
            ExecutionContext context,
            List<NodeExecutionRecord> history) {

        return WorkflowRunSnapshot.builder()
                .runId(runId)
                .state(state)
                .context(context)
                .nodeHistory(history)
                .snapshotTime(Instant.now())
                .metadata(Map.of("createdBy", "system", "type", "auto"))
                .build();
    }

    public static WorkflowRunSnapshot checkpoint(
            WorkflowRunId runId,
            WorkflowRunState state,
            ExecutionContext context,
            List<NodeExecutionRecord> history,
            String reason) {

        return WorkflowRunSnapshot.builder()
                .runId(runId)
                .state(state)
                .context(context)
                .nodeHistory(history)
                .snapshotTime(Instant.now())
                .isCheckpoint(true)
                .checkpointReason(reason)
                .metadata(Map.of(
                        "createdBy", "system",
                        "type", "checkpoint",
                        "checkpointReason", reason))
                .build();
    }

    public static WorkflowRunSnapshot forRecovery(
            WorkflowRunId runId,
            ExecutionContext context,
            List<NodeExecutionRecord> history) {

        return WorkflowRunSnapshot.builder()
                .runId(runId)
                .state(WorkflowRunState.RUNNING) // Assume running for recovery
                .context(context)
                .nodeHistory(history)
                .snapshotTime(Instant.now())
                .metadata(Map.of(
                        "createdBy", "recovery-system",
                        "type", "recovery",
                        "purpose", "workflow_recovery"))
                .build();
    }

    public NodeExecutionRecord getLastNodeExecution() {
        if (nodeHistory.isEmpty()) {
            return null;
        }
        return nodeHistory.get(nodeHistory.size() - 1);
    }

    public boolean hasNodeHistory() {
        return !nodeHistory.isEmpty();
    }

    public int getCompletedNodeCount() {
        return (int) nodeHistory.stream()
                .filter(record -> record.getStatus() == NodeExecutionStatus.COMPLETED)
                .count();
    }
}
