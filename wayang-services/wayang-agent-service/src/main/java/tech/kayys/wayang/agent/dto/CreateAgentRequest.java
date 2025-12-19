package tech.kayys.wayang.agent.dto;

public record CreateAgentRequest(
        String name,
        String description,
        String tenantId,
        AgentType agentType,
        LLMConfig llmConfig,
        java.util.List<String> tools,
        java.util.Map<String, Object> config) {
}