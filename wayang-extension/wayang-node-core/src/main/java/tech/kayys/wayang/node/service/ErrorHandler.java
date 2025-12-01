package tech.kayys.wayang.node.core.error;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.node.core.audit.NodeAuditLogger;
import tech.kayys.wayang.node.core.exception.NodeException;
import tech.kayys.wayang.node.core.model.*;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * Handles error recovery and retry logic for node executions.
 * 
 * Implements exponential backoff and circuit breaker patterns.
 */
@ApplicationScoped
public class ErrorHandler {
    
    private static final Logger LOG = LoggerFactory.getLogger(ErrorHandler.class);
    
    private final NodeAuditLogger auditLogger;
    private final MeterRegistry meterRegistry;
    
    @Inject
    public ErrorHandler(
        NodeAuditLogger auditLogger,
        MeterRegistry meterRegistry
    ) {
        this.auditLogger = auditLogger;
        this.meterRegistry = meterRegistry;
    }
    
    /**
     * Execute with retry logic
     */
    public <T> CompletionStage<T> executeWithRetry(
        String nodeId,
        String executionId,
        RetryPolicy policy,
        Supplier<CompletionStage<T>> execution
    ) {
        return executeWithRetry(nodeId, executionId, policy, execution, 1);
    }
    
    /**
     * Execute with retry logic (internal)
     */
    private <T> CompletionStage<T> executeWithRetry(
        String nodeId,
        String executionId,
        RetryPolicy policy,
        Supplier<CompletionStage<T>> execution,
        int attempt
    ) {
        LOG.debug("Executing node {} (attempt {}/{})", 
            nodeId, attempt, policy.maxAttempts());
        
        Instant startTime = Instant.now();
        
        return execution.get()
            .handle((result, throwable) -> {
                if (throwable == null) {
                    // Success
                    recordSuccess(nodeId, attempt, startTime);
                    return CompletableFuture.completedFuture(result);
                }
                
                // Extract root cause
                Throwable rootCause = getRootCause(throwable);
                
                // Check if retryable
                if (!isRetryable(rootCause) || attempt >= policy.maxAttempts()) {
                    recordFailure(nodeId, executionId, attempt, rootCause);
                    return CompletableFuture.<T>failedFuture(throwable);
                }
                
                // Calculate delay
                long delayMs = calculateDelay(policy, attempt);
                
                LOG.warn("Node execution failed (attempt {}/{}), retrying in {}ms: {}", 
                    attempt, policy.maxAttempts(), delayMs, rootCause.getMessage());
                
                meterRegistry.counter("node.execution.retry",
                    "node", nodeId,
                    "attempt", String.valueOf(attempt)
                ).increment();
                
                // Schedule retry
                return CompletableFuture
                    .delayedExecutor(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .execute(() -> {})
                    .thenCompose(v -> executeWithRetry(
                        nodeId, executionId, policy, execution, attempt + 1
                    ));
            })
            .thenCompose(stage -> stage);
    }
    
    /**
     * Handle error and create error info
     */
    public ErrorInfo handleError(
        String nodeId,
        String executionId,
        Throwable throwable
    ) {
        Throwable rootCause = getRootCause(throwable);
        
        String errorCode = determineErrorCode(rootCause);
        boolean retryable = isRetryable(rootCause);
        
        ErrorInfo errorInfo = new ErrorInfo(
            errorCode,
            rootCause.getMessage(),
            rootCause.getClass().getSimpleName(),
            retryable,
            Map.of(
                "rootCause", rootCause.getClass().getName(),
                "timestamp", Instant.now().toString()
            ),
            getStackTraceAsString(rootCause)
        );
        
        // Log to audit
        auditLogger.logExecutionFailed(
            executionId,
            errorCode,
            rootCause.getMessage(),
            retryable
        );
        
        // Record metrics
        meterRegistry.counter("node.execution.error",
            "node", nodeId,
            "error_code", errorCode,
            "retryable", String.valueOf(retryable)
        ).increment();
        
        return errorInfo;
    }
    
    /**
     * Create execution result from error
     */
    public ExecutionResult createErrorResult(
        String taskId,
        ErrorInfo errorInfo,
        Instant startTime
    ) {
        return new ExecutionResult(
            taskId,
            ExecutionStatus.FAILED,
            Map.of(),
            java.util.Optional.of(errorInfo),
            java.util.List.of(),
            new ExecutionMetrics(
                Duration.between(startTime, Instant.now()),
                0,
                0,
                0,
                0.0,
                Map.of()
            ),
            Map.of(),
            startTime,
            Instant.now()
        );
    }
    
    /**
     * Check if error is retryable
     */
    private boolean isRetryable(Throwable throwable) {
        // Check if it's a NodeException with retry flag
        if (throwable instanceof NodeException ne) {
            return ne.isRetryable();
        }
        
        // Transient errors are retryable
        String message = throwable.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            return lowerMessage.contains("timeout") ||
                   lowerMessage.contains("connection") ||
                   lowerMessage.contains("unavailable") ||
                   lowerMessage.contains("temporary");
        }
        
        return false;
    }
    
    /**
     * Determine error code from throwable
     */
    private String determineErrorCode(Throwable throwable) {
        if (throwable instanceof NodeException ne) {
            return ne.getErrorCode();
        }
        
        String className = throwable.getClass().getSimpleName();
        return "ERROR_" + className.toUpperCase().replace("EXCEPTION", "");
    }
    
    /**
     * Calculate retry delay with exponential backoff
     */
    private long calculateDelay(RetryPolicy policy, int attempt) {
        if (!policy.exponentialBackoff()) {
            return policy.initialDelayMs();
        }
        
        long delay = (long) (policy.initialDelayMs() * 
            Math.pow(policy.backoffMultiplier(), attempt - 1));
        
        // Add jitter (±25%)
        double jitter = 0.75 + (Math.random() * 0.5);
        delay = (long) (delay * jitter);
        
        return Math.min(delay, policy.maxDelayMs());
    }
    
    /**
     * Get root cause of exception
     */
    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }
    
    /**
     * Convert stack trace to string
     */
    private String getStackTraceAsString(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        throwable.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }
    
    /**
     * Record successful execution
     */
    private void recordSuccess(String nodeId, int attempt, Instant startTime) {
        meterRegistry.counter("node.execution.success",
            "node", nodeId,
            "attempts", String.valueOf(attempt)
        ).increment();
        
        meterRegistry.timer("node.execution.duration",
            "node", nodeId,
            "status", "success"
        ).record(Duration.between(startTime, Instant.now()));
    }
    
    /**
     * Record failed execution
     */
    private void recordFailure(
        String nodeId,
        String executionId,
        int attempt,
        Throwable throwable
    ) {
        meterRegistry.counter("node.execution.failure",
            "node", nodeId,
            "attempts", String.valueOf(attempt),
            "error_type", throwable.getClass().getSimpleName()
        ).increment();
        
        LOG.error("Node execution failed after {} attempts: {}", 
            attempt, nodeId, throwable);
    }
}