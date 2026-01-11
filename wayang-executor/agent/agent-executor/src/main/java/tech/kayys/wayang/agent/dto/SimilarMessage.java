package tech.kayys.wayang.agent.dto;

import java.util.Map;

import tech.kayys.wayang.agent.model.Message;

public record SimilarMessage(
        String id,
        Message message,
        double similarity,
        Map<String, Object> metadata) {
}