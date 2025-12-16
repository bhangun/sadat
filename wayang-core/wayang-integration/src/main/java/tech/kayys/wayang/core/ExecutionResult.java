



import lombok.Builder;
import lombok.Data;
import tech.kayys.wayang.error.ErrorPayload;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Result of node execution containing outputs, status, and metadata.
 */
@Data
@Builder
public class ExecutionResult {
    
    private ExecutionStatus status;
    
    @Builder.Default
    private Map<String, Object> outputs = new HashMap<>();
    
    private ErrorPayload error;
    
    private Instant startTime;
    private Instant endTime;
    
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    private String checkpointRef;
    private boolean requiresHumanReview;
    
    /**
     * Create success result
     */
    public static ExecutionResult success(Object output) {
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("result", output);
        
        return ExecutionResult.builder()
            .status(ExecutionStatus.SUCCESS)
            .outputs(outputs)
            .endTime(Instant.now())
            .build();
    }
    
    /**
     * Create success result with multiple outputs
     */
    public static ExecutionResult success(Map<String, Object> outputs) {
        return ExecutionResult.builder()
            .status(ExecutionStatus.SUCCESS)
            .outputs(outputs)
            .endTime(Instant.now())
            .build();
    }
    
    /**
     * Create error result
     */
    public static ExecutionResult error(ErrorPayload error) {
        return ExecutionResult.builder()
            .status(ExecutionStatus.FAILED)
            .error(error)
            .endTime(Instant.now())
            .build();
    }
    
    /**
     * Create result requiring human review
     */
    public static ExecutionResult needsReview(Object output, String reason) {
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("result", output);
        outputs.put("reviewReason", reason);
        
        return ExecutionResult.builder()
            .status(ExecutionStatus.PENDING_REVIEW)
            .outputs(outputs)
            .requiresHumanReview(true)
            .endTime(Instant.now())
            .build();
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
    public boolean isFailed() {
        return status == ExecutionStatus.FAILED;
    }
    
    /**
     * Get execution duration
     */
    public Duration getDuration() {
        if (startTime != null && endTime != null) {
            return Duration.between(startTime, endTime);
        }
        return Duration.ZERO;
    }
    
    /**
     * Add output value
     */
    public void addOutput(String name, Object value) {
        outputs.put(name, value);
    }
    
    /**
     * Get output value
     */
    public Object getOutput(String name) {
        return outputs.get(name);
    }
}