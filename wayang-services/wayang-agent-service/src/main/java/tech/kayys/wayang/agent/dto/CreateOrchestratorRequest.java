package tech.kayys.wayang.agent.dto;

public record CreateOrchestratorRequest(
        String name,
        String description,
        String tenantId,
        String strategy) {
}