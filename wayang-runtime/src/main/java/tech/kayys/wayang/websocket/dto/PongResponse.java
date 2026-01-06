package tech.kayys.wayang.websocket.dto;

import java.time.Instant;

public record PongResponse(
        String type,
        Instant timestamp) {
}
