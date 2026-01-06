package tech.kayys.wayang.workflow.kernel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * 🔒 Execution context with strong typing.
 * Opaque to kernel - plugins interpret variables.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionContext {

    private String executionId;
    private String workflowRunId;
    private String nodeId;
    private String tenantId;
    private Map<String, Object> variables;
    private Map<String, Object> metadata;
    private Map<String, Object> workflowState;
    private Instant createdAt;
    private Instant lastUpdatedAt;

    public String getRunId() {
        return workflowRunId;
    }

    // Strongly typed variables
    @SuppressWarnings("unchecked")
    public <T> T getVariable(String name, Class<T> type) {
        if (variables == null)
            return null;
        Object val = variables.get(name);
        return (T) val;
    }

    public <T> T getVariableOrDefault(String name, T defaultValue, Class<T> type) {
        T val = getVariable(name, type);
        return val != null ? val : defaultValue;
    }

    // Variable manipulation (returns new context)
    public ExecutionContext withVariable(String name, Object value, String type) {
        if (variables == null)
            variables = new java.util.HashMap<>();
        variables.put(name, value);
        return this;
    }

    public ExecutionContext withoutVariable(String name) {
        if (variables != null) {
            variables.remove(name);
        }
        return this;
    }

    public ExecutionContext withMetadata(String key, Object value) {
        if (metadata == null)
            metadata = new java.util.HashMap<>();
        metadata.put(key, value);
        return this;
    }

    public ExecutionContext withWorkflowState(Map<String, Object> updates) {
        if (workflowState == null)
            workflowState = new java.util.HashMap<>();
        workflowState.putAll(updates);
        return this;
    }
}
