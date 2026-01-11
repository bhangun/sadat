package tech.kayys.silat.executor.subworkflow;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Security validator for cross-tenant access
 */
@ApplicationScoped
class SubWorkflowSecurityValidator {

    private static final Logger LOG = LoggerFactory.getLogger(SubWorkflowSecurityValidator.class);

    @Inject
    tech.kayys.silat.api.security.TenantContext tenantContext;

    /**
     * Validate cross-tenant access
     */
    public void validateCrossTenantAccess(String sourceTenantId, String targetTenantId) {
        LOG.debug("Validating cross-tenant access: source={}, target={}",
            sourceTenantId, targetTenantId);

        // Check if source tenant has permission to invoke workflows in target tenant
        // This could check:
        // - Tenant relationships (parent-child, partners)
        // - Access control policies
        // - Service agreements

        if (!hasPermission(sourceTenantId, targetTenantId)) {
            throw new SecurityException(
                String.format("Tenant %s does not have permission to access tenant %s",
                    sourceTenantId, targetTenantId));
        }
    }

    private boolean hasPermission(String sourceTenantId, String targetTenantId) {
        // In production, this would check actual permissions
        // For now, allow if same tenant
        return sourceTenantId.equals(targetTenantId);
    }
}