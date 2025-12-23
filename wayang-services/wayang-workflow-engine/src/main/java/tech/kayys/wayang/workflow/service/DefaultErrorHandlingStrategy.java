package tech.kayys.wayang.workflow.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.schema.node.NodeDefinition;
import tech.kayys.wayang.schema.execution.ErrorPayload;
import tech.kayys.wayang.workflow.model.ExecutionContext;

/**
 * DefaultErrorHandlingStrategy - Default implementation of error handling strategy
 * 
 * This implementation provides configurable error handling based on node configuration
 * including retry policies, fallback nodes, and compensation actions.
 */
@ApplicationScoped
public class DefaultErrorHandlingStrategy implements ErrorHandlingStrategy {

    @Override
    public String getStrategyType() {
        return "DEFAULT";
    }

    @Override
    public boolean canHandle(ErrorPayload error, NodeDefinition nodeDef) {
        // This strategy can handle any error for any node
        return true;
    }

    @Override
    public boolean appliesTo(NodeDefinition nodeDef) {
        // This strategy applies to all nodes by default
        return true;
    }

    @Override
    public Uni<ErrorHandlingResult> handleError(NodeDefinition nodeDef, ErrorPayload error, ExecutionContext context) {
        // Check if the node has specific error handling configuration
        if (nodeDef.getErrorHandling() != null) {
            var errorHandling = nodeDef.getErrorHandling();

            // Check if retry policy is configured
            if (errorHandling.getRetryPolicy() != null) {
                int maxRetries = errorHandling.getRetryPolicy().getMaxAttempts() != null
                    ? errorHandling.getRetryPolicy().getMaxAttempts() : 3;

                // Check if we've exceeded max retries
                String retryCountKey = "retry_count_" + nodeDef.getId();
                int currentRetries = context.getVariable(retryCountKey) != null
                    ? (Integer) context.getVariable(retryCountKey) : 0;

                if (currentRetries < maxRetries) {
                    // Increment retry count
                    context.setVariable(retryCountKey, currentRetries + 1);

                    long delay = errorHandling.getRetryPolicy().getInitialDelayMs() != null
                        ? errorHandling.getRetryPolicy().getInitialDelayMs() : 1000;

                    // Apply exponential backoff if configured
                    if ("exponential".equals(errorHandling.getRetryPolicy().getBackoff())) {
                        delay = (long) (delay * Math.pow(2.0, currentRetries));
                    }

                    return Uni.createFrom().item(ErrorHandlingResult.retry(delay));
                }
            }

            // Check if fallback is configured
            if (errorHandling.getFallback() != null && errorHandling.getFallback().getNodeId() != null) {
                return Uni.createFrom().item(ErrorHandlingResult.fallback(errorHandling.getFallback().getNodeId()));
            }

            // Check if fallback node ID is configured (deprecated but kept for compatibility)
            if (errorHandling.getFallbackNodeId() != null && !errorHandling.getFallbackNodeId().isEmpty()) {
                return Uni.createFrom().item(ErrorHandlingResult.fallback(errorHandling.getFallbackNodeId()));
            }
        }

        // Default behavior: fail the workflow
        return Uni.createFrom().item(ErrorHandlingResult.fail("Node execution failed: " + error.getMessage()));
    }
}