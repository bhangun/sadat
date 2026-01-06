package tech.kayys.wayang.websocket.dto;

public record SubscribeResponse(
                String type,
                String channel,
                String resourceId) {
}