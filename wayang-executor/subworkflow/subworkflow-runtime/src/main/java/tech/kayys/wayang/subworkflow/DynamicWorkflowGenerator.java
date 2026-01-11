package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Dynamic Workflow Generation
 * Generate workflows programmatically based on templates or AI
 */
interface DynamicWorkflowGenerator {

    /**
     * Generate workflow from template with parameters
     */
    Uni<tech.kayys.silat.core.domain.WorkflowDefinition> generateFromTemplate(
        String templateId,
        Map<String, Object> parameters,
        tech.kayys.silat.core.domain.TenantId tenantId
    );

    /**
     * Generate workflow from natural language description (AI)
     */
    Uni<tech.kayys.silat.core.domain.WorkflowDefinition> generateFromDescription(
        String description,
        tech.kayys.silat.core.domain.TenantId tenantId
    );

    /**
     * Clone and modify existing workflow
     */
    Uni<tech.kayys.silat.core.domain.WorkflowDefinition> cloneAndModify(
        tech.kayys.silat.core.domain.WorkflowDefinitionId sourceId,
        List<WorkflowModification> modifications,
        tech.kayys.silat.core.domain.TenantId tenantId
    );
}