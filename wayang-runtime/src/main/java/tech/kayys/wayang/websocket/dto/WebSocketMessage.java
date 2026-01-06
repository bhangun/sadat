package tech.kayys.wayang.websocket.dto;

import java.time.Instant;
import java.util.Map;

public record WebSocketMessage(
                String type,
                String messageId,
                Instant timestamp,
                Map<String, Object> payload) {
}