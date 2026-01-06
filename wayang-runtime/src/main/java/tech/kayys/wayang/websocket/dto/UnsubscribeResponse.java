package tech.kayys.wayang.websocket.dto;

public record UnsubscribeResponse(
        String type,
        String channel,
        String resourceId) {
}