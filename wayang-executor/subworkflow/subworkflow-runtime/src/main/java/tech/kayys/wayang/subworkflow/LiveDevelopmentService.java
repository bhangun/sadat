package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Hot Reload & Live Development
 */
interface LiveDevelopmentService {

    /**
     * Enable hot reload for workflow
     */
    Uni<Void> enableHotReload(tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId);

    /**
     * Update workflow definition without stopping runs
     */
    Uni<Void> hotUpdate(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        tech.kayys.silat.core.domain.WorkflowDefinition newDefinition
    );

    /**
     * Debug mode with breakpoints
     */
    Uni<DebugSession> startDebugSession(tech.kayys.silat.core.domain.WorkflowRunId runId);
}