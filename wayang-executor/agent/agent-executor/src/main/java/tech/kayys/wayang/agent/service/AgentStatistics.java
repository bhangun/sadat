package tech.kayys.wayang.agent.service;

import java.util.Map;

/**
 * Aggregated agent statistics
 */
record AgentStatistics(
        long totalExecutions,
        long successfulExecutions,
        long totalTokens,
        double averageDurationMs) {

    public double successRate() {
        if (totalExecutions == 0)
            return 0.0;
        return (double) successfulExecutions / totalExecutions * 100.0;
    }

    public Map<String, Object> toMap() {
        return Map.of(
                "totalExecutions", totalExecutions,
                "successfulExecutions", successfulExecutions,
                "totalTokens", totalTokens,
                "averageDurationMs", averageDurationMs,
                "successRate", successRate());
    }
}