package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Fine-Grained Access Control (RBAC/ABAC)
 */
interface WorkflowAccessControlService {

    /**
     * Define role-based permissions
     */
    Uni<Void> defineRole(
        String roleName,
        List<Permission> permissions
    );

    /**
     * Assign role to user
     */
    Uni<Void> assignRole(
        String userId,
        String roleName,
        tech.kayys.silat.core.domain.TenantId tenantId
    );

    /**
     * Check permission
     */
    Uni<Boolean> hasPermission(
        String userId,
        Permission permission,
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId
    );

    /**
     * Attribute-based access control
     */
    Uni<Boolean> evaluatePolicy(
        String userId,
        Map<String, Object> context,
        AccessPolicy policy
    );
}