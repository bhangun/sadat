package tech.kayys.silat.executor.subworkflow;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.silat.core.domain.TenantId;
import tech.kayys.silat.core.domain.WorkflowDefinitionId;

/**
 * Sub-workflow registry
 */
@ApplicationScoped
class SubWorkflowRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(SubWorkflowRegistry.class);

    @Inject
    tech.kayys.silat.core.registry.WorkflowDefinitionRegistry definitionRegistry;

    /**
     * Verify workflow exists
     */
    public Uni<Boolean> verifyWorkflowExists(String workflowId, String tenantId) {
        return definitionRegistry.getDefinition(
                WorkflowDefinitionId.of(workflowId),
                TenantId.of(tenantId))
            .map(def -> true)
            .onFailure().recoverWithItem(false);
    }

    /**
     * Resolve workflow name to ID
     */
    public String resolveWorkflowId(String workflowName) {
        // This would query the registry to find workflow by name
        // For now, assume name == id
        return workflowName;
    }
}