package tech.kayys.wayang.agent.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AgentDefinition(
        String id,
        String name,
        String description,
        String tenantId,
        AgentType type,
        LLMConfig llmConfig,
        List<String> tools,
        Map<String, Object> config,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean isActive) {
}