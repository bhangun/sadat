package tech.kayys.wayang.websocket.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AgentEvent(
        String type,
        UUID agentId,
        String status,
        String message,
        Map<String, Object> data,
        Instant timestamp) {
}
