package tech.kayys.wayang.agent.dto;

public record AgentBenchmarkResult(
        String agentId,
        int iterations,
        long avgResponseTimeMs,
        double successRate,
        double tokensPerSecond,
        long startTime,
        long endTime) {
}