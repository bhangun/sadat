
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import tech.kayys.wayang.error.ErrorPayload;
import tech.kayys.wayang.error.ErrorSeverity;
import tech.kayys.wayang.nodes.NodeContext;

import javax.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Error handler for integration nodes.
 * Maps Camel exceptions to standardized ErrorPayload.
 */
@Slf4j
@ApplicationScoped
@RegisterForReflection
public class IntegrationErrorHandler {
    
    public ErrorPayload handleCamelException(Exception ex, NodeContext context) {
        log.error("Handling Camel exception for node {}", context.getNodeId(), ex);
        
        ErrorPayload.ErrorPayloadBuilder builder = ErrorPayload.builder()
            .type("IntegrationError")
            .originNode(context.getNodeId())
            .originRunId(context.getRunId())
            .timestamp(Instant.now());
        
        if (ex instanceof CamelExecutionException camelEx) {
            return handleCamelExecutionException(camelEx, context, builder);
        } else if (ex instanceof org.apache.camel.TypeConversionException) {
            return builder
                .subtype("TypeConversionError")
                .message("Failed to convert data type: " + ex.getMessage())
                .retryable(false)
                .severity(ErrorSeverity.ERROR)
                .suggestedAction("fix_data_format")
                .build();
        } else if (ex instanceof org.apache.camel.component.kafka.KafkaException) {
            return builder
                .subtype("KafkaError")
                .message(ex.getMessage())
                .retryable(true)
                .severity(ErrorSeverity.ERROR)
                .suggestedAction("retry")
                .build();
        } else if (ex instanceof java.net.SocketTimeoutException) {
            return builder
                .subtype("TimeoutError")
                .message("Connection timeout: " + ex.getMessage())
                .retryable(true)
                .severity(ErrorSeverity.WARNING)
                .suggestedAction("retry")
                .build();
        } else if (ex instanceof java.net.ConnectException) {
            return builder
                .subtype("ConnectionError")
                .message("Cannot connect to endpoint: " + ex.getMessage())
                .retryable(true)
                .severity(ErrorSeverity.ERROR)
                .suggestedAction("retry")
                .build();
        } else if (ex instanceof java.util.concurrent.TimeoutException) {
            return builder
                .subtype("ExecutionTimeout")
                .message("Route execution timeout: " + ex.getMessage())
                .retryable(true)
                .severity(ErrorSeverity.WARNING)
                .suggestedAction("retry")
                .build();
        } else {
            return builder
                .subtype("UnknownIntegrationError")
                .message(ex.getMessage())
                .retryable(true)
                .severity(ErrorSeverity.ERROR)
                .suggestedAction("escalate")
                .details(Map.of("exceptionClass", ex.getClass().getName()))
                .build();
        }
    }
    
    private ErrorPayload handleCamelExecutionException(CamelExecutionException camelEx, 
                                                       NodeContext context,
                                                       ErrorPayload.ErrorPayloadBuilder builder) {
        Exchange exchange = camelEx.getExchange();
        
        Map<String, Object> details = new HashMap<>();
        details.put("exchangeId", exchange.getExchangeId());
        
        if (exchange.getProperty(Exchange.FAILURE_ENDPOINT) != null) {
            details.put("failedEndpoint", exchange.getProperty(Exchange.FAILURE_ENDPOINT, String.class));
        }
        
        if (exchange.getFromRouteId() != null) {
            details.put("routeId", exchange.getFromRouteId());
        }
        
        details.put("camelErrorCode", camelEx.getStatusCode());
        
        String subtype = classifyError(camelEx);
        boolean retryable = isRetryable(camelEx);
        
        return builder
            .subtype(subtype)
            .message(camelEx.getMessage())
            .details(details)
            .retryable(retryable)
            .severity(retryable ? ErrorSeverity.WARNING : ErrorSeverity.ERROR)
            .suggestedAction(retryable ? "retry" : "escalate")
            .build();
    }
    
    private String classifyError(CamelExecutionException ex) {
        String message = ex.getMessage().toLowerCase();
        
        if (message.contains("marshal") || message.contains("unmarshal") || 
            message.contains("json") || message.contains("xml")) {
            return "TransformationError";
        } else if (message.contains("connect") || message.contains("timeout") || 
                   message.contains("socket")) {
            return "ConnectorError";
        } else if (message.contains("route") || message.contains("predicate") || 
                   message.contains("choice")) {
            return "RoutingError";
        } else if (message.contains("sql") || message.contains("database") || 
                   message.contains("jdbc")) {
            return "DatabaseError";
        } else if (message.contains("kafka") || message.contains("jms") || 
                   message.contains("amqp")) {
            return "MessagingError";
        } else if (message.contains("http") || message.contains("rest")) {
            return "HttpError";
        }
        
        return "UnknownIntegrationError";
    }
    
    private boolean isRetryable(CamelExecutionException ex) {
        String message = ex.getMessage().toLowerCase();
        
        // Not retryable: validation, schema, transformation errors
        if (message.contains("validation") || message.contains("schema") || 
            message.contains("marshal") || message.contains("unmarshal")) {
            return false;
        }
        
        // Retryable: network, timeout, temporary errors
        return message.contains("timeout") || message.contains("connect") || 
               message.contains("unavailable") || message.contains("temporary");
    }
}