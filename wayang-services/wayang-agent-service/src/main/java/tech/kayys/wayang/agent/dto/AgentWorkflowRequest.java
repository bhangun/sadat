package tech.kayys.wayang.agent.dto;

import java.util.List;

/**
 * Request to create agent workflow
 */
public record AgentWorkflowRequest(
        String name,
        String description,
        String tenantId,
        AgentType agentType,
        LLMConfig llmConfig,
        List<String> tools,
        RAGConfig ragConfig,
        MemoryConfig memoryConfig,
        List<NodeSpec> nodes,
        List<EdgeSpec> edges) {
}
