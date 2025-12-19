package tech.kayys.wayang.workflow.service;

import lombok.Builder;
import lombok.Data;
import tech.kayys.wayang.schema.execution.ErrorPayload;

import java.util.Map;

@Data
@Builder
public class NodeExecutionResult {
    private String nodeId;
    private Status status;
    private Map<String, Object> output; // Main output
    private Map<String, Object> outputChannels; // Per-port output
    private ErrorPayload error;
    private String blockReason;
    private String humanTaskId;

    public enum Status {
        SUCCESS, ERROR, BLOCKED, AWAITING_HUMAN, ABORTED
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isError() {
        return status == Status.ERROR;
    }

    public boolean isBlocked() {
        return status == Status.BLOCKED;
    }

    public boolean isAwaitingHuman() {
        return status == Status.AWAITING_HUMAN;
    }

    public boolean isAborted() {
        return status == Status.ABORTED;
    }

    public static NodeExecutionResult error(String nodeId, ErrorPayload error) {
        return NodeExecutionResult.builder()
                .nodeId(nodeId)
                .status(Status.ERROR)
                .error(error)
                .build();
    }

    public static NodeExecutionResult aborted(String nodeId, ErrorPayload error) {
        return NodeExecutionResult.builder()
                .nodeId(nodeId)
                .status(Status.ABORTED)
                .error(error)
                .build();
    }

    public static NodeExecutionResult awaitingHuman(String nodeId, String humanTaskId) {
        return NodeExecutionResult.builder()
                .nodeId(nodeId)
                .status(Status.AWAITING_HUMAN)
                .humanTaskId(humanTaskId)
                .build();
    }

    public static NodeExecutionResult success(String nodeId, Map<String, Object> output) {
        return NodeExecutionResult.builder()
                .nodeId(nodeId)
                .status(Status.SUCCESS)
                .output(output)
                .outputChannels(output) // Default to same if not specified
                .build();
    }
}
