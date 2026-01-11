package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: ML-Powered Workflow Optimization
 */
interface MLWorkflowOptimizer {

    /**
     * Predict workflow execution time
     */
    Uni<java.time.Duration> predictExecutionTime(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        Map<String, Object> inputs
    );

    /**
     * Recommend execution path from context
     */
    Uni<List<tech.kayys.silat.core.domain.NodeId>> recommendExecutionPath(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        Map<String, Object> context
    );

    /**
     * Predict failure probability
     */
    Uni<FailurePrediction> predictFailure(
        tech.kayys.silat.core.domain.WorkflowRunId runId
    );

    /**
     * Auto-tune retry policies based on history
     */
    Uni<RetryPolicy> optimizeRetryPolicy(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        tech.kayys.silat.core.domain.NodeId nodeId
    );
}