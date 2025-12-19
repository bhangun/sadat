package tech.kayys.wayang.schema;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.util.concurrent.ExecutionError;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * ExecutionStatus - Detailed execution status
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutionStatus {

    private String id;
    private String workflowId;
    private ExecutionStatusEnum status;
    private Instant startedAt;
    private Instant completedAt;
    private Duration duration;

    private Map<String, NodeExecutionStatus> nodeStatuses = new HashMap<>();
    private Map<String, Object> outputs;
    private ExecutionError error;
    private ExecutionMetrics metrics;

    // Getters and setters...
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ExecutionStatusEnum getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatusEnum status) {
        this.status = status;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public Map<String, Object> getOutputs() {
        return outputs;
    }

    public void setOutputs(Map<String, Object> outputs) {
        this.outputs = outputs;
    }

    public ExecutionError getError() {
        return error;
    }

    public void setError(ExecutionError error) {
        this.error = error;
    }

    public ExecutionMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(ExecutionMetrics metrics) {
        this.metrics = metrics;
    }

    public Map<String, NodeExecutionStatus> getNodeStatuses() {
        return nodeStatuses;
    }

    public void setNodeStatuses(Map<String, NodeExecutionStatus> nodeStatuses) {
        this.nodeStatuses = nodeStatuses;
    }
}
