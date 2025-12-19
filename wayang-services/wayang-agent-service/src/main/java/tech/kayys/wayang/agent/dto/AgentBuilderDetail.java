package tech.kayys.wayang.agent.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AgentBuilderDetail(
        String id,
        String name,
        String description,
        String tenantId,
        AgentType agentType,
        Boolean isActive,
        LLMConfig llmConfig,
        List<String> tools,
        Map<String, Object> config,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String status,
        List<AgentVersionInfo> versions,
        AgentMetrics metrics) {
}

record AgentVersionInfo(
    String version,
    String description,
    java.time.LocalDateTime createdAt,
    String createdBy
) {}