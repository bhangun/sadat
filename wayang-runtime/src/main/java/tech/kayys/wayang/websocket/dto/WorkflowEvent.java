package tech.kayys.wayang.websocket.dto;

import java.time.Instant;
import java.util.Map;

public record WorkflowEvent(
        String type,
        String runId,
        String status,
        Map<String, Object> data,
        Instant timestamp) {
}
