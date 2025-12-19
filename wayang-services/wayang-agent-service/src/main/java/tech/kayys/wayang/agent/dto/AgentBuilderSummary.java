package tech.kayys.wayang.agent.dto;

import java.time.LocalDateTime;

public record AgentBuilderSummary(
    String id,
    String name,
    String description,
    AgentType agentType,
    Boolean isActive,
    LocalDateTime createdAt
) {}