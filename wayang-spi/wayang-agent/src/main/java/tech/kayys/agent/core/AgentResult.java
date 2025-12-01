package tech.kayys.agent.core;

/**
 * Result of agent execution
 */
@Immutable
public final class AgentResult {
    private final ExecutionStatus status;
    private final Map<String, Object> outputs;
    private final List<AgentEvent> events;
    private final Optional<Throwable> error;
    private final ExecutionMetrics metrics;
    private final ProvenanceRecord provenance;

    private AgentResult(Builder builder) {
        this.status = requireNonNull(builder.status);
        this.outputs = Map.copyOf(builder.outputs);
        this.events = List.copyOf(builder.events);
        this.error = builder.error;
        this.metrics = builder.metrics;
        this.provenance = builder.provenance;
    }

    public boolean isSuccess() {
        return status == ExecutionStatus.SUCCESS;
    }

    public boolean requiresRetry() {
        return status == ExecutionStatus.RETRY;
    }

    public boolean requiresEscalation() {
        return status == ExecutionStatus.ESCALATE;
    }

    public static Builder success() {
        return new Builder().status(ExecutionStatus.SUCCESS);
    }

    public static Builder failure(Throwable error) {
        return new Builder()
                .status(ExecutionStatus.FAILED)
                .error(error);
    }

    public static class Builder {
        private ExecutionStatus status;
        private Map<String, Object> outputs = new HashMap<>();
        private List<AgentEvent> events = new ArrayList<>();
        private Optional<Throwable> error = Optional.empty();
        private ExecutionMetrics metrics;
        private ProvenanceRecord provenance;

        public Builder status(ExecutionStatus status) {
            this.status = status;
            return this;
        }

        public Builder output(String key, Object value) {
            this.outputs.put(key, value);
            return this;
        }

        public Builder event(AgentEvent event) {
            this.events.add(event);
            return this;
        }

        // Additional builder methods...

        public AgentResult build() {
            return new AgentResult(this);
        }
    }
}