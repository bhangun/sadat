package tech.kayys.wayang.agent;

import java.util.Set;

/**
 * Orchestrator Agent - Coordinates multiple agents
 * Contains built-in sub-agents for planning, execution, and evaluation
 */
public record OrchestratorAgent(
    OrchestrationType orchestrationType,
    BuiltInAgents builtInAgents,
    CoordinationStrategy coordinationStrategy,
    int maxConcurrentAgents,
    boolean supportsRecursiveOrchestration
) implements AgentType {
    
    @Override
    public String typeName() {
        return "ORCHESTRATOR_AGENT";
    }
    
    @Override
    public Set<AgentCapability> requiredCapabilities() {
        return Set.of(
            AgentCapability.ORCHESTRATION,
            AgentCapability.PLANNING,
            AgentCapability.COORDINATION,
            AgentCapability.EVALUATION
        );
    }
    
    @Override
    public AgentRole role() {
        return AgentRole.ORCHESTRATOR;
    }
}
