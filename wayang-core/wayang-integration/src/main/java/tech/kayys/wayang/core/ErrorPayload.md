



import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Standardized error payload used across all nodes.
 * Implements error-as-input pattern.
 */
@Data
@Builder
public class ErrorPayload {
    
    private String type;
    private String subtype;
    private String message;
    
    @Builder.Default
    private Map<String, Object> details = new HashMap<>();
    
    private boolean retryable;
    private String originNode;
    private String originRunId;
    
    @Builder.Default
    private int attempt = 0;
    
    private int maxAttempts;
    private Instant timestamp;
    private String suggestedAction;
    private String provenanceRef;
    private ErrorSeverity severity;
    
    /**
     * Create integration error
     */
    public static ErrorPayload integrationError(String message, String subtype, boolean retryable) {
        return ErrorPayload.builder()
            .type("IntegrationError")
            .subtype(subtype)
            .message(message)
            .retryable(retryable)
            .timestamp(Instant.now())
            .severity(ErrorSeverity.ERROR)
            .build();
    }
    
    /**
     * Create validation error
     */
    public static ErrorPayload validationError(String message) {
        return ErrorPayload.builder()
            .type("ValidationError").message(message)
            .retryable(false)
            .timestamp(Instant.now())
            .severity(ErrorSeverity.ERROR)
            .suggestedAction("fix_input")
            .build();
    }
    
    /**
     * Create timeout error
     */
    public static ErrorPayload timeoutError(String nodeId, long timeoutMs) {
        return ErrorPayload.builder()
            .type("TimeoutError")
            .message("Node execution exceeded timeout: " + timeoutMs + "ms")
            .originNode(nodeId)
            .retryable(true)
            .timestamp(Instant.now())
            .severity(ErrorSeverity.WARNING)
            .suggestedAction("retry")
            .build();
    }
    
    /**
     * Check if error should be escalated to human
     */
    public boolean shouldEscalate() {
        return severity == ErrorSeverity.CRITICAL || 
               (attempt >= maxAttempts && !retryable);
    }
    
    /**
     * Increment attempt counter
     */
    public void incrementAttempt() {
        this.attempt++;
    }
}