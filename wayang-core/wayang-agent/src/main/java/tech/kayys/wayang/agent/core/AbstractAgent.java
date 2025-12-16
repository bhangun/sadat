package tech.kayys.agent.core;

/**
 * Abstract base implementation providing common agent functionality
 */
public abstract class AbstractAgent implements Agent {
    protected final String id;
    protected final AgentType type;
    protected final Set<Capability> capabilities;
    protected AgentConfig config;
    protected volatile HealthStatus healthStatus;
    protected final AgentMetrics metrics;

    protected AbstractAgent(String id, AgentType type, Set<Capability> capabilities) {
        this.id = id;
        this.type = type;
        this.capabilities = Collections.unmodifiableSet(capabilities);
        this.metrics = new AgentMetrics(id);
        this.healthStatus = HealthStatus.INITIALIZING;
    }

    @Override
    public void initialize(AgentConfig config) throws AgentInitializationException {
        this.config = config;
        doInitialize(config);
        this.healthStatus = HealthStatus.HEALTHY;
    }

    /**
     * Template method for subclass-specific initialization
     */
    protected abstract void doInitialize(AgentConfig config) throws AgentInitializationException;

    @Override
    public AgentResult execute(AgentContext context) throws AgentExecutionException {
        validateContext(context);
        metrics.recordExecution();

        try {
            return doExecute(context);
        } catch (Exception e) {
            metrics.recordFailure();
            throw new AgentExecutionException("Agent execution failed", e);
        }
    }

    /**
     * Template method for subclass-specific execution logic
     */
    protected abstract AgentResult doExecute(AgentContext context) throws AgentExecutionException;

    protected void validateContext(AgentContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        // Additional validation logic
    }
}