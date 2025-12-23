package tech.kayys.wayang.workflow.service;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.schema.node.NodeDefinition;
import tech.kayys.wayang.schema.execution.ErrorPayload;
import tech.kayys.wayang.workflow.model.ExecutionContext;

/**
 * ErrorHandlingStrategy - Interface for different error handling approaches
 * 
 * This interface allows for different error handling strategies (retry, fallback, compensation, etc.)
 * making the workflow engine more flexible and use case agnostic.
 */
public interface ErrorHandlingStrategy {
    
    /**
     * Handle an error during node execution
     * @param nodeDef The node definition where the error occurred
     * @param error The error payload
     * @param context The execution context
     * @return A Uni containing the error handling result
     */
    Uni<ErrorHandlingResult> handleError(NodeDefinition nodeDef, ErrorPayload error, ExecutionContext context);
    
    /**
     * Get the strategy type identifier
     * @return The strategy type
     */
    String getStrategyType();
    
    /**
     * Check if this strategy can handle a specific error
     * @param error The error to check
     * @param nodeDef The node where error occurred
     * @return true if this strategy can handle the error
     */
    boolean canHandle(ErrorPayload error, NodeDefinition nodeDef);
    
    /**
     * Check if the strategy should be applied to a specific node
     * @param nodeDef The node definition
     * @return true if the strategy applies to this node
     */
    boolean appliesTo(NodeDefinition nodeDef);
}

/**
 * ErrorHandlingResult - Result of error handling operation
 */
class ErrorHandlingResult {
    private final ErrorHandlingAction action;
    private final Object result;
    private final String fallbackNodeId;
    private final long retryDelayMs;
    private final boolean shouldRetry;
    private final boolean shouldContinue;
    private final String errorMessage;
    
    public ErrorHandlingResult(ErrorHandlingAction action, Object result, String fallbackNodeId, 
                             long retryDelayMs, boolean shouldRetry, boolean shouldContinue, String errorMessage) {
        this.action = action;
        this.result = result;
        this.fallbackNodeId = fallbackNodeId;
        this.retryDelayMs = retryDelayMs;
        this.shouldRetry = shouldRetry;
        this.shouldContinue = shouldContinue;
        this.errorMessage = errorMessage;
    }
    
    // Getters
    public ErrorHandlingAction getAction() { return action; }
    public Object getResult() { return result; }
    public String getFallbackNodeId() { return fallbackNodeId; }
    public long getRetryDelayMs() { return retryDelayMs; }
    public boolean shouldRetry() { return shouldRetry; }
    public boolean shouldContinue() { return shouldContinue; }
    public String getErrorMessage() { return errorMessage; }
    
    // Convenience factory methods
    public static ErrorHandlingResult retry(long delayMs) {
        return new ErrorHandlingResult(ErrorHandlingAction.RETRY, null, null, delayMs, true, false, null);
    }
    
    public static ErrorHandlingResult fallback(String fallbackNodeId) {
        return new ErrorHandlingResult(ErrorHandlingAction.FALLBACK, null, fallbackNodeId, 0, false, false, null);
    }
    
    public static ErrorHandlingResult fail(String errorMessage) {
        return new ErrorHandlingResult(ErrorHandlingAction.FAIL, null, null, 0, false, false, errorMessage);
    }
    
    public static ErrorHandlingResult continueWith(Object result) {
        return new ErrorHandlingResult(ErrorHandlingAction.CONTINUE, result, null, 0, false, true, null);
    }
    
    public static ErrorHandlingResult compensate(String compensationAction) {
        return new ErrorHandlingResult(ErrorHandlingAction.COMPENSATE, null, compensationAction, 0, false, false, null);
    }
}

/**
 * ErrorHandlingAction - Enum for different error handling actions
 */
enum ErrorHandlingAction {
    RETRY,      // Retry the operation
    FALLBACK,   // Execute fallback node
    FAIL,       // Fail the workflow
    CONTINUE,   // Continue with a result
    COMPENSATE  // Execute compensation action
}