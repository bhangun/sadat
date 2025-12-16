



import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.extern.slf4j.Slf4j;
import tech.kayys.wayang.audit.AuditService;
import tech.kayys.wayang.nodes.NodeContext;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Central error handler for all nodes.
 * Maps exceptions to ErrorPayload and triggers appropriate actions.
 */
@Slf4j
@ApplicationScoped
@RegisterForReflection
public class ErrorHandler {
    
    @Inject
    AuditService auditService;
    
    /**
     * Handle exception and create ErrorPayload
     */
    public ErrorPayload handle(Exception ex, NodeContext context) {
        log.error("Error in node {}: {}", context.getNodeId(), ex.getMessage(), ex);
        
        ErrorPayload error = mapException(ex, context);
        
        // Record in audit log
        auditService.recordError(context, error);
        
        return error;
    }
    
    /**
     * Map exception to ErrorPayload
     */
    private ErrorPayload mapException(Exception ex, NodeContext context) {
        ErrorPayload.ErrorPayloadBuilder builder = ErrorPayload.builder()
            .originNode(context.getNodeId())
            .originRunId(context.getRunId())
            .timestamp(java.time.Instant.now());
        
        if (ex instanceof IllegalArgumentException) {
            return builder
                .type("ValidationError")
                .message(ex.getMessage())
                .retryable(false)
                .severity(ErrorSeverity.ERROR)
                .suggestedAction("fix_input")
                .build();
        } else if (ex instanceof java.net.SocketTimeoutException) {
            return builder
                .type("TimeoutError")
                .message(ex.getMessage())
                .retryable(true)
                .severity(ErrorSeverity.WARNING)
                .suggestedAction("retry")
                .build();
        } else if (ex instanceof java.net.ConnectException) {
            return builder
                .type("NetworkError")
                .message(ex.getMessage())
                .retryable(true)
                .severity(ErrorSeverity.ERROR)
                .suggestedAction("retry")
                .build();
        } else {
            return builder
                .type("UnknownError")
                .message(ex.getMessage())
                .retryable(true)
                .severity(ErrorSeverity.ERROR)
                .suggestedAction("escalate")
                .build();
        }
    }
}