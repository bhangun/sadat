package tech.kayys.wayang.workflow.security.context;

import tech.kayys.wayang.workflow.security.model.ExecutionContext;
import io.quarkus.security.identity.SecurityIdentity;

/**
 * Thread-local storage for security context during request processing.
 * Provides access to tenant ID, user identity, and execution context throughout
 * the request lifecycle.
 */
public class SecurityContextHolder {

    private static final ThreadLocal<SecurityContext> CONTEXT = new ThreadLocal<>();

    /**
     * Set the security context for the current thread
     */
    public static void setContext(SecurityContext context) {
        CONTEXT.set(context);
    }

    /**
     * Get the security context for the current thread
     */
    public static SecurityContext getContext() {
        SecurityContext context = CONTEXT.get();
        if (context == null) {
            throw new SecurityException("No security context available");
        }
        return context;
    }

    /**
     * Get the current tenant ID
     */
    public static String getCurrentTenantId() {
        return getContext().tenantId();
    }

    /**
     * Get the current execution context (may be null for control plane operations)
     */
    public static ExecutionContext getExecutionContext() {
        return getContext().executionContext();
    }

    /**
     * Clear the security context (should be called after request completes)
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * Check if a security context is set
     */
    public static boolean hasContext() {
        return CONTEXT.get() != null;
    }

    /**
     * Security context containing both user and execution information
     */
    public record SecurityContext(
            String tenantId,
            SecurityIdentity identity,
            ExecutionContext executionContext,
            String principalType) { // execution_token, user_jwt, service_account

        /**
         * Create a context from user authentication (control plane)
         */
        public static SecurityContext fromUserAuth(String tenantId, SecurityIdentity identity) {
            return new SecurityContext(tenantId, identity, null, "USER_JWT");
        }

        /**
         * Create a context from service account (internal/infra)
         */
        public static SecurityContext fromServiceAccount(String tenantId, SecurityIdentity identity) {
            return new SecurityContext(tenantId, identity, null, "SERVICE_ACCOUNT");
        }

        /**
         * Create a context from execution token (execution plane)
         */
        public static SecurityContext fromExecutionContext(ExecutionContext executionContext) {
            return new SecurityContext(executionContext.tenantId(), null, executionContext, "EXECUTION_TOKEN");
        }
    }
}
