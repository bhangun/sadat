package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Batch Processing
 */
interface BatchWorkflowProcessor {

    /**
     * Submit batch of workflow executions
     */
    Uni<BatchSubmission> submitBatch(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        List<Map<String, Object>> inputsList,
        BatchConfig config
    );

    /**
     * Monitor batch progress
     */
    Uni<BatchProgress> getBatchProgress(String batchId);

    /**
     * Cancel entire batch
     */
    Uni<Void> cancelBatch(String batchId);
}