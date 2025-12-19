package tech.kayys.wayang.agent.dto;

import java.util.List;
import java.util.Map;

public record AgentDetailResponse(
        String id,
        String name,
        String description,
        String tenantId,
        AgentType agentType,
        String status,
        Map<String, Object> config,
        List<String> tools,
        List<AgentVersionInfo> versions,
        AgentMetrics metrics,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt) {
    
    public static AgentDetailResponse from(Object agent, Object workflow, AgentMetrics metrics, Object versions) {
        // Placeholder implementation
        return new AgentDetailResponse(
            "placeholder-id",
            "placeholder-name",
            "placeholder-description", 
            "placeholder-tenant",
            AgentType.AI_AGENT,
            "ACTIVE",
            Map.of(),
            List.of(),
            List.of(), // versions
            metrics,
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now()
        );
    }
}

record AgentVersionInfo(
    String version,
    String description,
    java.time.LocalDateTime createdAt,
    String createdBy
) {}