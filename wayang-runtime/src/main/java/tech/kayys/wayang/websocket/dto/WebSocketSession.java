package tech.kayys.wayang.websocket.dto;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * WebSocket session information
 */
public record WebSocketSession(
                String connectionId,
                String tenantId,
                String userId,
                String email,
                Set<String> permissions,
                Instant connectedAt,
                Map<String, Object> metadata) {
}
