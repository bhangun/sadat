package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Workflow Caching Strategy
 */
interface WorkflowCacheService {

    /**
     * Cache workflow definition
     */
    Uni<Void> cacheDefinition(tech.kayys.silat.core.domain.WorkflowDefinition definition);

    /**
     * Cache execution results (memoization)
     */
    Uni<Void> cacheExecutionResult(
        tech.kayys.silat.core.domain.WorkflowRunId runId,
        tech.kayys.silat.core.domain.NodeId nodeId,
        Map<String, Object> inputs,
        Map<String, Object> outputs
    );

    /**
     * Get cached result
     */
    Uni<Optional<Map<String, Object>>> getCachedResult(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        tech.kayys.silat.core.domain.NodeId nodeId,
        Map<String, Object> inputs
    );

    /**
     * Invalidate cache
     */
    Uni<Void> invalidateCache(tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId);
}