package tech.kayys.wayang.agent.dto;

public record RateLimitStatus(
        int capacity,
        int available,
        int totalRequests) {
}