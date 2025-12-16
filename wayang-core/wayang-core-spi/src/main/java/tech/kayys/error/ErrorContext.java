package tech.kayys.error;

import CircuitBreakerState;
import NodeContext;

/**
 * Error Context for policy evaluation
 */
class ErrorContext {
    private ErrorPayload error;
    private NodeContext nodeContext;
    private CircuitBreakerState circuitBreakerState;

    public ErrorPayload getError() {
        return error;
    }

    public void setError(ErrorPayload error) {
        this.error = error;
    }

    public NodeContext getNodeContext() {
        return nodeContext;
    }

    public void setNodeContext(NodeContext nodeContext) {
        this.nodeContext = nodeContext;
    }

    public CircuitBreakerState getCircuitBreakerState() {
        return circuitBreakerState;
    }

    public void setCircuitBreakerState(CircuitBreakerState circuitBreakerState) {
        this.circuitBreakerState = circuitBreakerState;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ErrorPayload error;
        private NodeContext nodeContext;
        private CircuitBreakerState circuitBreakerState;

        public Builder error(ErrorPayload error) {
            this.error = error;
            return this;
        }

        public Builder nodeContext(NodeContext nodeContext) {
            this.nodeContext = nodeContext;
            return this;
        }

        public Builder circuitBreakerState(CircuitBreakerState circuitBreakerState) {
            this.circuitBreakerState = circuitBreakerState;
            return this;
        }

        public ErrorContext build() {
            ErrorContext context = new ErrorContext();
            context.error = this.error;
            context.nodeContext = this.nodeContext;
            context.circuitBreakerState = this.circuitBreakerState;
            return context;
        }
    }
}