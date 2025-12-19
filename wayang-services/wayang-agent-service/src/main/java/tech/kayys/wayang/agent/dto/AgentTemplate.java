package tech.kayys.wayang.agent.dto;

import java.util.List;
import java.util.Map;

public record AgentTemplate(
        String id,
        String name,
        String description,
        String category,
        AgentType agentType,
        List<String> tools,
        Map<String, Object> parameters) {
}