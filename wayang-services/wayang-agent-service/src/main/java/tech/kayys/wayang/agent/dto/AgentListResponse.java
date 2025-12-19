package tech.kayys.wayang.agent.dto;

import java.util.List;

public record AgentListResponse(
        List<AgentSummary> agents,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}

record AgentSummary(
    String id,
    String name,
    String description,
    AgentType agentType,
    String status,
    java.time.LocalDateTime createdAt
) {
    public static AgentSummary from(Object agent) {
        return new AgentSummary(
            "placeholder-id",
            "placeholder-name", 
            "placeholder-description",
            AgentType.AI_AGENT,
            "ACTIVE",
            java.time.LocalDateTime.now()
        );
    }
}