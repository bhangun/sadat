package tech.kayys.wayang.websocket.dto;

import java.time.Instant;

public record WelcomeMessage(
                String type,
                String tenantId,
                String userId,
                Instant connectedAt) {
}