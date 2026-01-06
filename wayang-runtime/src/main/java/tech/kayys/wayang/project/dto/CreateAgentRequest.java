package tech.kayys.wayang.project.dto;

import java.util.List;

public record CreateAgentRequest(
        String agentName,
        String description,
        AgentType agentType,
        LLMConfig llmConfig,
        List<AgentCapability> capabilities,
        List<AgentTool> tools,
        MemoryConfig memoryConfig,
        List<Guardrail> guardrails) {
}
