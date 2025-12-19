package tech.kayys.wayang.agent.dto;

import java.util.List;
import java.util.Map;

public record AgentResponse(
        String id,
        String name,
        String description,
        String tenantId,
        AgentType agentType,
        String status,
        Map<String, Object> config,
        List<String> tools,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt) {
    
    public static AgentResponse from(Object agent, Object workflow) {
        // Placeholder implementation
        return new AgentResponse(
            "placeholder-id",
            "placeholder-name", 
            "placeholder-description",
            "placeholder-tenant",
            AgentType.AI_AGENT,
            "ACTIVE",
            Map.of(),
            List.of(),
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now()
        );
    }
}