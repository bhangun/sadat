package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Natural Language Workflow Builder
 */
interface NLPWorkflowBuilder {

    /**
     * Build workflow from natural language
     */
    Uni<tech.kayys.silat.core.domain.WorkflowDefinition> buildFromNL(
        String description,
        tech.kayys.silat.core.domain.TenantId tenantId
    );

    /**
     * Suggest next node from context
     */
    Uni<List<NodeSuggestion>> suggestNextNode(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        tech.kayys.silat.core.domain.NodeId currentNode,
        String intent
    );

    /**
     * Explain workflow in natural language
     */
    Uni<String> explainWorkflow(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId
    );
}