package tech.kayys.wayang.agent.dto;

import java.util.List;
import java.util.Map;

public record AgentBuilderRequest(
        String name,
        String description,
        String tenantId,
        AgentType agentType,
        LLMConfig llmConfig,
        List<String> tools,
        Map<String, Object> config) {
}