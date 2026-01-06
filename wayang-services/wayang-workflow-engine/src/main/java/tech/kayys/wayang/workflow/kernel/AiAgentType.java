package tech.kayys.wayang.workflow.kernel;

import java.util.Arrays;
import java.util.Set;

/**
 * AI Agent types with specific capabilities
 */
public enum AiAgentType {

    // Basic Conversation Agents
    CHAT_AGENT("chat", Set.of(
            "conversation", "qa", "summarization", "translation")),

    // Retrieval-Augmented Generation
    RAG_AGENT("rag", Set.of(
            "document_retrieval", "knowledge_query", "contextual_response")),

    // Planning and Orchestration
    PLANNER_AGENT("planner", Set.of(
            "task_decomposition", "workflow_generation", "strategy_planning")),

    // Tool-Using Agents
    TOOL_AGENT("tool", Set.of(
            "function_calling", "api_integration", "code_execution")),

    // Multi-Agent Orchestrator
    ORCHESTRATOR_AGENT("orchestrator", Set.of(
            "agent_coordination", "workflow_orchestration", "dynamic_routing")),

    // Specialized Agents
    CODE_AGENT("code", Set.of(
            "code_generation", "code_review", "debugging")),

    ANALYTICS_AGENT("analytics", Set.of(
            "data_analysis", "insight_generation", "prediction")),

    // Composite Agents (combine multiple capabilities)
    COMPOSITE_AGENT("composite", Set.of(
            "multi_modal", "hybrid_reasoning", "sequential_processing"));

    private final String type;
    private final Set<String> capabilities;

    AiAgentType(String type, Set<String> capabilities) {
        this.type = type;
        this.capabilities = capabilities;
    }

    public String getType() {
        return type;
    }

    public Set<String> getCapabilities() {
        return capabilities;
    }

    public boolean supports(String capability) {
        return capabilities.contains(capability);
    }

    public static AiAgentType fromString(String type) {
        return Arrays.stream(values())
                .filter(t -> t.type.equalsIgnoreCase(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown AI agent type: " + type));
    }
}
