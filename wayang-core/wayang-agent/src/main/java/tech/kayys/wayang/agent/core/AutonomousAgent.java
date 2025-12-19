package tech.kayys.wayang.agent.core;

/**
 * Specialized agent for autonomous reasoning and planning
 */
public abstract class AutonomousAgent extends AbstractAgent {
    protected final ReasoningEngine reasoningEngine;
    protected final MemoryService memoryService;

    protected AutonomousAgent(String id, Set<Capability> capabilities,
            ReasoningEngine reasoningEngine,
            MemoryService memoryService) {
        super(id, AgentType.AUTONOMOUS, capabilities);
        this.reasoningEngine = reasoningEngine;
        this.memoryService = memoryService;
    }

    /**
     * Plan execution strategy based on goal
     */
    protected abstract ExecutionPlan plan(Goal goal, AgentContext context);

    /**
     * Reflect on execution results and learn
     */
    protected abstract void reflect(AgentResult result, AgentContext context);
}