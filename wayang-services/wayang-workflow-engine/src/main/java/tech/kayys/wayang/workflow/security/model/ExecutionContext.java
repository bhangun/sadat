package tech.kayys.wayang.workflow.security.model;

import java.time.Instant;
import java.util.Set;

/**
 * Execution context containing authorization and tenant information for
 * workflow execution.
 * This is the internal security token used by the execution plane.
 * 
 * Based on the security model from docs/concern.md:
 * - Control Plane uses user JWTs
 * - Execution Plane uses signed execution context tokens
 */
public record ExecutionContext(
        String runId,
        String workflowId,
        String tenantId,
        String environment,
        Initiator initiator,
        Set<Permission> permissions,
        Instant issuedAt,
        Instant expiresAt) {

    /**
     * Check if this execution context has a specific permission
     */
    public boolean hasPermission(Permission requiredPermission) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        return permissions.stream().anyMatch(p -> p.implies(requiredPermission));
    }

    /**
     * Check if this execution context is expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Validate the execution context
     */
    public void validate() {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("Tenant ID is required");
        }
        if (runId == null || runId.isBlank()) {
            throw new IllegalStateException("Run ID is required");
        }
        if (workflowId == null || workflowId.isBlank()) {
            throw new IllegalStateException("Workflow ID is required");
        }
        if (isExpired()) {
            throw new SecurityException("Execution context has expired");
        }
    }
}
