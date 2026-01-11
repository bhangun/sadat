package tech.kayys.wayang.agent.dto;

public record PerformanceStats(
        long totalExecutions,
        long slowExecutions,
        long highTokenUsage,
        long excessiveIterations,
        double avgDurationMs,
        double avgTokens) {
}
