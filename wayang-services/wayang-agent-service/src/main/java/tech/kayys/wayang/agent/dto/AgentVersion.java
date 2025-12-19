package tech.kayys.wayang.agent.dto;

public record AgentVersion(
        String version,
        long timestamp,
        String description,
        String createdBy) {
}