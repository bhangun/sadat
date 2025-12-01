package tech.kayys.wayang.node.core.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Result of node execution, including outputs, status, metrics, and events.
 * Immutable and thread-safe.
 */
public record ExecutionResult(
    String taskId,
    ExecutionStatus status,
    Map<String, Object> outputs,
    Optional<ErrorInfo> error,
    List<ExecutionEvent> events,
    ExecutionMetrics metrics,
    Map<String, Object> provenanceMetadata,
    Instant startTime,
    Instant endTime
) {
    
    public ExecutionResult {
        outputs = outputs != null ? Map.copyOf(outputs) : Map.of();
        events = events != null ? List.copyOf(events) : List.of();
        provenanceMetadata = provenanceMetadata != null ? Map.copyOf(provenanceMetadata) : Map.of();
    }
    
    /**
     * Check if execution was successful
     */
    public boolean isSuccess() {
        return status == ExecutionStatus.SUCCESS;
    }
    
    /**
     * Check if execution failed
     */
    public boolean isFailure() {
        return status == ExecutionStatus.FAILED || status == ExecutionStatus.TIMEOUT;
    }
    
    /**
     * Get duration of execution
     */
    public Duration getDuration() {
        return Duration.between(startTime, endTime);
    }
    
    /**
     * Get output value
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getOutput(String name, Class<T> type) {
        Object value = outputs.get(name);
        if (value == null) {
            return Optional.empty();
        }
        if (type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }
    
    /**
     * Create a successful result
     */
    public static ExecutionResult success(
        String taskId,
        Map<String, Object> outputs,
        ExecutionMetrics metrics,
        Instant startTime,
        Instant endTime
    ) {
        return new ExecutionResult(
            taskId,
            ExecutionStatus.SUCCESS,
            outputs,
            Optional.empty(),
            List.of(),
            metrics,
            Map.of(),
            startTime,
            endTime
        );
    }
    
    /**
     * Create a failed result
     */
    public static ExecutionResult failure(
        String taskId,
        ErrorInfo error,
        ExecutionMetrics metrics,
        Instant startTime,
        Instant endTime
    ) {
        return new ExecutionResult(
            taskId,
            ExecutionStatus.FAILED,
            Map.of(),
            Optional.of(error),
            List.of(),
            metrics,
            Map.of(),
            startTime,
            endTime
        );
    }
}