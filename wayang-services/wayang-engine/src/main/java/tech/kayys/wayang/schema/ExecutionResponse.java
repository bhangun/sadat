package tech.kayys.wayang.schema;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * ExecutionResponse - Response from execution request
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutionResponse {

    @NotBlank
    private String id; // Execution ID

    @NotBlank
    private String workflowId;

    @NotNull
    private ExecutionStatusEnum status;

    private Instant startedAt;
    private Map<String, Object> outputs;
    private String message;

    // Getters and setters...
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public ExecutionStatusEnum getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatusEnum status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Map<String, Object> getOutputs() {
        return outputs;
    }

    public void setOutputs(Map<String, Object> outputs) {
        this.outputs = outputs;
    }
}