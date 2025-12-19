package tech.kayys.wayang.agent.dto;

public record ConnectionTestResult(
        boolean success,
        String message,
        String endpoint,
        long latencyMs) {
}
