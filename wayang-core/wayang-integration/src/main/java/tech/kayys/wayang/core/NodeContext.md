
import lombok.Builder;
import lombok.Data;
import tech.kayys.wayang.error.ErrorPayload;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Context object passed to nodes during execution.
 * Contains inputs, variables, metadata, and execution state.
 */
@Data
@Builder
public class NodeContext {
    
    private String runId;
    private String nodeId;
    private String workflowId;
    private String tenantId;
    private String userId;
    
    @Builder.Default
    private Map<String, Object> inputs = new HashMap<>();
    
    @Builder.Default
    private Map<String, Object> variables = new HashMap<>();
    
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    private Instant startTime;
    private String traceId;
    private String parentSpanId;
    
    // Error context (for error-as-input pattern)
    private ErrorPayload previousError;
    private boolean isRetry;
    private int attemptNumber;
    
    /**
     * Get input value by name
     */
    public Object getInput(String name) {
        return inputs.get(name);
    }
    
    /**
     * Get input value with type casting
     */
    @SuppressWarnings("unchecked")
    public <T> T getInput(String name, Class<T> type) {
        Object value = inputs.get(name);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        throw new ClassCastException("Cannot cast " + value.getClass() + " to " + type);
    }
    
    /**
     * Set variable
     */
    public void setVariable(String name, Object value) {
        variables.put(name, value);
    }
    
    /**
     * Get variable
     */
    public Object getVariable(String name) {
        return variables.get(name);
    }
    
    /**
     * Create context for child node
     */
    public NodeContext createChildContext(String childNodeId) {
        return NodeContext.builder()
            .runId(runId)
            .nodeId(childNodeId)
            .workflowId(workflowId)
            .tenantId(tenantId)
            .userId(userId)
            .variables(new HashMap<>(variables))
            .metadata(new HashMap<>(metadata))
            .traceId(traceId)
            .parentSpanId(nodeId)
            .startTime(Instant.now())
            .build();
    }
    
    /**
     * Factory method for creating context
     */
    public static NodeContext create(String nodeId, Map<String, Object> inputs) {
        return NodeContext.builder()
            .runId(UUID.randomUUID().toString())
            .nodeId(nodeId)
            .inputs(inputs)
            .startTime(Instant.now())
            .traceId(UUID.randomUUID().toString())
            .build();
    }
}