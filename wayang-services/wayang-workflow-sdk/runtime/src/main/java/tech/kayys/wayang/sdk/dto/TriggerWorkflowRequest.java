package tech.kayys.wayang.sdk.dto;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;




/**
 * Request to trigger a new workflow execution
 */
public record TriggerWorkflowRequest(
    String workflowId,
    String workflowVersion,
    Map<String, Object> inputs,
    String correlationId
) {
    public TriggerWorkflowRequest {
        if (workflowId == null || workflowId.isBlank()) {
            throw new IllegalArgumentException("workflowId is required");
        }
        if (workflowVersion == null || workflowVersion.isBlank()) {
            throw new IllegalArgumentException("workflowVersion is required");
        }
        inputs = inputs != null ? Map.copyOf(inputs) : Map.of();
    }
}
