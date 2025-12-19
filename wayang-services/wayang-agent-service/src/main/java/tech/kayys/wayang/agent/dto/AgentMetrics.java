package tech.kayys.wayang.agent.dto;

public record AgentMetrics(
        long durationMs,
        int tokensUsed,
        double cost) {
}