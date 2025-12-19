package tech.kayys.wayang.agent.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AgentBuilderResponse(
        String id,
        String name,
        String description,
        String tenantId,
        AgentType agentType,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String status) {
}