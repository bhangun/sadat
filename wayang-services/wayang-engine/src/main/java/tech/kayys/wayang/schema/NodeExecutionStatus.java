package tech.kayys.wayang.schema;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.util.concurrent.ExecutionError;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * NodeExecutionStatus - Status of individual node execution
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeExecutionStatus {

    private String nodeId;
    private ExecutionStatusEnum status;
    private Instant startedAt;
    private Instant completedAt;
    private int attempts;
    private ExecutionError error;

    // Getters and setters...
    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public ExecutionStatusEnum getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatusEnum status) {
        this.status = status;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public ExecutionError getError() {
        return error;
    }

    public void setError(ExecutionError error) {
        this.error = error;
    }
}
