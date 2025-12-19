package tech.kayys.wayang.sdk.dto;

import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;





/**
 * Workflow execution event (for SSE streaming)
 */
public record WorkflowExecutionEvent(
    String id,
    String runId,
    Long sequence,
    EventType type,
    Map<String, Object> data,
    Instant timestamp
) {
    public enum EventType {
        CREATED,
        STATUS_CHANGED,
        NODE_EXECUTED,
        STATE_UPDATED,
        RESUMED,
        CANCELLED
    }
}
