package tech.kayys.wayang.guardrails.dto;

public record RateLimitConfig(
        int requestsPerMinute) {
}
