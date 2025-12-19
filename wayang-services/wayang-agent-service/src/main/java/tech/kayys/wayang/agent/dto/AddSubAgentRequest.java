package tech.kayys.wayang.agent.dto;

public record AddSubAgentRequest(
        String subAgentId,
        String configuration,
        String relationshipType) {
}