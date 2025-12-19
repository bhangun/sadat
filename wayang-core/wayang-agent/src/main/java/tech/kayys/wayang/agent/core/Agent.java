package tech.kayys.wayang.agent.core;

/**
 * Base interface for all agent types in the Wayang platform.
 * Defines the fundamental contract that all agents must implement.
 * 
 * Design principles:
 * - Stateless execution model
 * - Context-based operation
 * - Observable lifecycle
 * - Capability declaration
 */
public interface Agent {
    /**
     * Unique identifier for this agent instance
     */
    String getId();

    /**
     * Agent type identifier (e.g., "planner", "evaluator", "specialist")
     */
    AgentType getType();

    /**
     * Declared capabilities of this agent
     */
    Set<Capability> getCapabilities();

    /**
     * Execute the agent's primary function
     * 
     * @param context Execution context containing inputs, state, and resources
     * @return Result of agent execution including outputs and metadata
     * @throws AgentExecutionException if execution fails
     */
    AgentResult execute(AgentContext context) throws AgentExecutionException;

    /**
     * Initialize agent with configuration
     * Called once during agent lifecycle setup
     */
    void initialize(AgentConfig config) throws AgentInitializationException;

    /**
     * Cleanup resources when agent is being destroyed
     */
    void destroy();

    /**
     * Health check for agent availability
     */
    HealthStatus checkHealth();
}