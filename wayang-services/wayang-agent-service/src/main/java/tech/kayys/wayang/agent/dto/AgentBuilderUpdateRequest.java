package tech.kayys.wayang.agent.dto;

import java.util.List;
import java.util.Map;

public record AgentBuilderUpdateRequest(
        String name,
        String description,
        AgentType agentType,
        LLMConfig llmConfig,
        List<String> tools,
        Map<String, Object> config,
        Boolean isActive) {
}