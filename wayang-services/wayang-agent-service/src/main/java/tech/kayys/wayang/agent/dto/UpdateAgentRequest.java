package tech.kayys.wayang.agent.dto;

import java.util.List;
import java.util.Map;

public record UpdateAgentRequest(
        String name,
        String description,
        AgentType agentType,
        LLMConfig llmConfig,
        List<String> tools,
        Map<String, Object> config,
        Boolean isActive) {
}