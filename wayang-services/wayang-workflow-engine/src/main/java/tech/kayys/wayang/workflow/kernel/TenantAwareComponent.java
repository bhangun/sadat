package tech.kayys.wayang.workflow.kernel;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.workflow.security.context.SecurityContextHolder;
import java.util.HashMap;

/**
 * Shared tenant validation and security utilities
 */
public abstract class TenantAwareComponent {

    protected void verifyTenantAccess(String tenantId) {
        if (SecurityContextHolder.hasContext()) {
            String authenticatedTenant = SecurityContextHolder.getCurrentTenantId();
            if (!authenticatedTenant.equals(tenantId)) {
                throw new SecurityException(
                        String.format("Access denied: Tenant mismatch. Expected %s, got %s",
                                tenantId, authenticatedTenant));
            }
        }
        // For internal/system calls without context, we rely on caller authentication
    }

    protected <T> Uni<T> validateNotNull(String name, Object value, Uni<T> next) {
        if (value == null) {
            return Uni.createFrom().failure(
                    new IllegalArgumentException(String.format("%s cannot be null", name)));
        }
        return next;
    }

    protected <T> Uni<T> validateNotEmpty(String name, String value, Uni<T> next) {
        if (value == null || value.trim().isEmpty()) {
            return Uni.createFrom().failure(
                    new IllegalArgumentException(String.format("%s cannot be null or empty", name)));
        }
        return next;
    }

    protected ExecutionContext createSafeExecutionContext(ExecutionContext context) {
        return ExecutionContext.builder()
                .variables(context.getVariables() != null ? new HashMap<>(context.getVariables()) : new HashMap<>())
                .metadata(context.getMetadata() != null ? new HashMap<>(context.getMetadata()) : new HashMap<>())
                .tenantId(context.getTenantId())
                .workflowRunId(context.getWorkflowRunId())
                .nodeId(context.getNodeId())
                .build();
    }
}