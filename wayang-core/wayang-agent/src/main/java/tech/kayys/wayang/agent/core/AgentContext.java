package tech.kayys.agent.core;

/**
 * Immutable context passed to agents during execution.
 * Contains all necessary information for agent operation.
 */
@Immutable
public final class AgentContext {
    private final String runId;
    private final String workflowId;
    private final String tenantId;
    private final Map<String, Object> inputs;
    private final Map<String, Object> state;
    private final ExecutionMetadata metadata;
    private final ResourceBindings resources;
    private final SecurityContext securityContext;

    private AgentContext(Builder builder) {
        this.runId = requireNonNull(builder.runId);
        this.workflowId = requireNonNull(builder.workflowId);
        this.tenantId = requireNonNull(builder.tenantId);
        this.inputs = Map.copyOf(builder.inputs);
        this.state = Map.copyOf(builder.state);
        this.metadata = builder.metadata;
        this.resources = builder.resources;
        this.securityContext = builder.securityContext;
    }

    public <T> Optional<T> getInput(String key, Class<T> type) {
        return Optional.ofNullable(inputs.get(key))
                .map(type::cast);
    }

    public <T> T getRequiredInput(String key, Class<T> type) {
        return getInput(key, type)
                .orElseThrow(() -> new IllegalStateException("Required input missing: " + key));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String runId;
        private String workflowId;
        private String tenantId;
        private Map<String, Object> inputs = new HashMap<>();
        private Map<String, Object> state = new HashMap<>();
        private ExecutionMetadata metadata;
        private ResourceBindings resources;
        private SecurityContext securityContext;

        public Builder runId(String runId) {
            this.runId = runId;
            return this;
        }

        public Builder input(String key, Object value) {
            this.inputs.put(key, value);
            return this;
        }

        public Builder inputs(Map<String, Object> inputs) {
            this.inputs.putAll(inputs);
            return this;
        }

        // Additional builder methods...

        public AgentContext build() {
            return new AgentContext(this);
        }
    }
}
