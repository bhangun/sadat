package tech.kayys.wayang.sdk.dto;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;



/**
 * Request to inject an event into workflow
 */
public record WorkflowEventRequest(
    String eventType,
    String correlationKey,
    Map<String, Object> payload,
    Map<String, String> metadata
) {
    public WorkflowEventRequest {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType is required");
        }
        payload = payload != null ? Map.copyOf(payload) : Map.of();
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public WorkflowEventRequest(String eventType, String correlationKey, Map<String, Object> payload) {
        this(eventType, correlationKey, payload, Map.of());
    }
}
