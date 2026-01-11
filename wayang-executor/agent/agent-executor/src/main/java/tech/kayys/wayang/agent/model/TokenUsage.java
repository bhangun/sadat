package tech.kayys.wayang.agent.model;

import java.util.Map;

/**
 * Token usage statistics
 */
public record TokenUsage(
        int promptTokens,
        int completionTokens,
        int totalTokens) {

    public Map<String, Object> toMap() {
        return Map.of(
                "promptTokens", promptTokens,
                "completionTokens", completionTokens,
                "totalTokens", totalTokens);
    }

    public static TokenUsage of(int promptTokens, int completionTokens) {
        return new TokenUsage(
                promptTokens,
                completionTokens,
                promptTokens + completionTokens);
    }
}
