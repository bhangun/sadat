package tech.kayys.wayang.websocket.dto;

public record CommandResponse(
        String type,
        String command,
        String targetId) {
}
