package tech.kayys.silat.execution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import tech.kayys.silat.model.NodeId;
import tech.kayys.silat.model.TenantId;
import tech.kayys.silat.model.WorkflowRunId;
import tech.kayys.silat.model.event.ExecutionEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 🔒 Execution context with strong typing.
 * Opaque to kernel - plugins interpret variables.
 */
@Data
@Builder
@AllArgsConstructor
public class ExecutionContext {

    private String executionId;
    private String workflowRunId;
    private String nodeId;
    private Map<String, Object> variables;
    private Map<String, Object> metadata;
    private Map<String, Object> workflowState;
    private Instant createdAt;
    private Instant lastUpdatedAt;

    private final WorkflowRunId runId;
    private final TenantId tenantId;
    private final Map<NodeId, NodeExecutionState> nodeStates;
    private final List<ExecutionEvent> events;
    private Instant startedAt;
    private Instant completedAt;

    public ExecutionContext(WorkflowRunId runId, TenantId tenantId, Map<String, Object> initialVariables) {
        this.runId = Objects.requireNonNull(runId);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.variables = new HashMap<>(initialVariables);
        this.nodeStates = new HashMap<>();
        this.events = new ArrayList<>();
    }

    public WorkflowRunId getRunId() {
        return runId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    public Object getVariable(String key) {
        return variables.get(key);
    }

    public Map<String, Object> getVariables() {
        return Collections.unmodifiableMap(variables);
    }

    public void updateNodeState(NodeId nodeId, NodeExecutionState state) {
        nodeStates.put(nodeId, state);
    }

    public Optional<NodeExecutionState> getNodeState(NodeId nodeId) {
        return Optional.ofNullable(nodeStates.get(nodeId));
    }

    public Map<NodeId, NodeExecutionState> getAllNodeStates() {
        return Collections.unmodifiableMap(nodeStates);
    }

    public void recordEvent(ExecutionEvent event) {
        events.add(event);
    }

    public List<ExecutionEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public void markStarted() {
        this.startedAt = Instant.now();
    }

    public void markCompleted() {
        this.completedAt = Instant.now();
    }

    public Optional<Instant> getStartedAt() {
        return Optional.ofNullable(startedAt);
    }

    public Optional<Instant> getCompletedAt() {
        return Optional.ofNullable(completedAt);
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
