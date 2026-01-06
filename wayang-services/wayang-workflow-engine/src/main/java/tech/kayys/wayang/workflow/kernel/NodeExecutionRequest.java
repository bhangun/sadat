package tech.kayys.wayang.workflow.kernel;

import java.util.Map;
import java.util.Objects;

/**
 * Request for executing a node, used for batch operations
 */
public class NodeExecutionRequest {

    private final String requestId;
    private final ExecutionContext context;
    private final NodeDescriptor node;
    private final ExecutionToken token;
    private final Map<String, Object> additionalParams;
    private final Priority priority;

    public NodeExecutionRequest(String requestId, ExecutionContext context,
            NodeDescriptor node, ExecutionToken token,
            Map<String, Object> additionalParams, Priority priority) {
        this.requestId = Objects.requireNonNull(requestId, "requestId cannot be null");
        this.context = Objects.requireNonNull(context, "context cannot be null");
        this.node = Objects.requireNonNull(node, "node cannot be null");
        this.token = Objects.requireNonNull(token, "token cannot be null");
        this.additionalParams = additionalParams != null ? Map.copyOf(additionalParams) : Map.of();
        this.priority = priority != null ? priority : Priority.NORMAL;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public String getRequestId() {
        return requestId;
    }

    public ExecutionContext getContext() {
        return context;
    }

    public NodeDescriptor getNode() {
        return node;
    }

    public ExecutionToken getToken() {
        return token;
    }

    public Map<String, Object> getAdditionalParams() {
        return additionalParams;
    }

    public Priority getPriority() {
        return priority;
    }

    public enum Priority {
        LOW, NORMAL, HIGH, CRITICAL
    }

    public static class Builder {
        private String requestId;
        private ExecutionContext context;
        private NodeDescriptor node;
        private ExecutionToken token;
        private Map<String, Object> additionalParams;
        private Priority priority;

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder context(ExecutionContext context) {
            this.context = context;
            return this;
        }

        public Builder node(NodeDescriptor node) {
            this.node = node;
            return this;
        }

        public Builder token(ExecutionToken token) {
            this.token = token;
            return this;
        }

        public Builder additionalParams(Map<String, Object> additionalParams) {
            this.additionalParams = additionalParams;
            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public NodeExecutionRequest build() {
            return new NodeExecutionRequest(
                    requestId, context, node, token, additionalParams, priority);
        }
    }
}
