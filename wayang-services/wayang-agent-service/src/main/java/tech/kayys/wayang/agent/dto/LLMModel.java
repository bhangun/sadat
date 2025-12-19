package tech.kayys.wayang.agent.dto;

import java.util.List;

public record LLMModel(
        String id,
        String name,
        int contextWindow,
        List<String> capabilities) {
}