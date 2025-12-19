package tech.kayys.wayang.agent.dto;

public record LLMConfig(
        String provider,
        String model,
        double temperature,
        int maxTokens) {
}
