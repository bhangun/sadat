package tech.kayys.wayang.agent.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Sandbox for safe tool execution
 */
@ApplicationScoped
public class ToolExecutionSandbox {

    private static final Logger LOG = LoggerFactory.getLogger(ToolExecutionSandbox.class);

    @Inject
    SecurityContext securityContext;

    /**
     * Execute tool with security checks
     */
    public <T> Uni<T> executeSecurely(
            String toolName,
            java.util.function.Supplier<Uni<T>> execution) {

        // Check if tool is allowed for current user
        if (!isToolAllowed(toolName)) {
            LOG.warn("Tool execution denied: {} for user: {}",
                    toolName, securityContext.getCurrentUserId());
            return Uni.createFrom().failure(
                    new SecurityException("Tool execution not permitted: " + toolName));
        }

        // Check rate limits for tool
        if (!checkToolRateLimit(toolName)) {
            LOG.warn("Tool rate limit exceeded: {}", toolName);
            return Uni.createFrom().failure(
                    new SecurityException("Tool rate limit exceeded"));
        }

        // Execute with timeout
        return execution.get()
                .ifNoItem().after(Duration.ofSeconds(30))
                .failWith(new java.util.concurrent.TimeoutException("Tool execution timeout"))
                .onItem().invoke(result -> auditToolExecution(toolName, true, null))
                .onFailure().invoke(error -> auditToolExecution(toolName, false, error.getMessage()));
    }

    private boolean isToolAllowed(String toolName) {
        // Check permissions for tool
        String permission = "tool:" + toolName;
        return securityContext.hasPermission(permission) ||
                securityContext.hasPermission("tool:*");
    }

    private boolean checkToolRateLimit(String toolName) {
        // Implement tool-specific rate limiting
        return true; // Placeholder
    }

    private void auditToolExecution(String toolName, boolean success, String error) {
        // Log tool execution for audit
        LOG.info("Tool execution: tool={}, user={}, tenant={}, success={}, error={}",
                toolName,
                securityContext.getCurrentUserId(),
                securityContext.getCurrentTenantId(),
                success,
                error);
    }
}