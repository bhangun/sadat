package tech.kayys.wayang.workflow.kernel;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Flow.Subscription;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.workflow.api.model.RunStatus;

/**
 * WorkflowRunManager with distributed coordination capabilities
 */
public interface DistributedWorkflowRunManager extends WorkflowRunManager, ManagedWorkflowComponent {

    /**
     * Create a run with distributed lock
     */
    Uni<WorkflowRunId> createRunWithLock(
            WorkflowDescriptor workflow,
            Map<String, Object> inputs,
            RunTrigger trigger,
            DistributedLockConfig lockConfig);

    /**
     * Handle node result with optimistic concurrency control
     */
    Uni<Void> handleNodeResultWithOCC(
            WorkflowRunId runId,
            NodeExecutionResult result,
            long expectedVersion);

    /**
     * Subscribe to run state changes across cluster
     */
    Uni<Subscription> subscribeToRunChanges(
            WorkflowRunId runId,
            RunStateChangeListener listener);

    /**
     * Get cluster-wide run statistics
     */
    Uni<ClusterRunStats> getClusterRunStats(String tenantId);

    /**
     * Replicate run state to another node
     */
    Uni<Void> replicateRunState(WorkflowRunId runId, String targetNodeId);

    public interface RunStateChangeListener {
        void onStateChanged(WorkflowRunId runId, WorkflowRunState newState);

        void onNodeExecuted(WorkflowRunId runId, String nodeId, NodeExecutionResult result);
    }

    public record DistributedLockConfig(
            String lockKey,
            Duration timeout,
            Duration leaseTime) {
    }

    public record ClusterRunStats(
            int totalNodes,
            int activeNodes,
            Map<String, Integer> runsPerNode,
            Map<RunStatus, Long> statusDistribution) {
    }
}
