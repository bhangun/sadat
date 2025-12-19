package tech.kayys.wayang.node.model;

/**
 * Node execution result container.
 */
@lombok.Data
@lombok.Builder(toBuilder = true)
class NodeExecutionResult {
    private final String nodeId;
    private final ExecutionStatus status;
    private final Map<String, Object> outputChannels;
    private final ErrorPayload error;
    private final String blockReason;
    private final String humanTaskId;
    private final Map<String, Object> metadata;

    public static NodeExecutionResult success(
            String nodeId,
            Map<String, Object> outputs) {
        return NodeExecutionResult.builder()
                .nodeId(nodeId)
                .status(ExecutionStatus.SUCCESS)
                .outputChannels(outputs)
                .metadata(new HashMap<>())
                .build();
    }

    public static NodeExecutionResult error(String nodeId, ErrorPayload error) {
        return NodeExecutionResult.builder()
                .nodeId(nodeId)
                .status(ExecutionStatus.ERROR)
                .error(error)
                .outputChannels(new HashMap<>())
                .metadata(new HashMap<>())
                .build();
    }

    public static NodeExecutionResult blocked(String nodeId, String reason) {
        return NodeExecutionResult.builder()
                .nodeId(nodeId)
                .status(ExecutionStatus.BLOCKED)
                .blockReason(reason)
                .outputChannels(new HashMap<>())
                .metadata(new HashMap<>())
                .build();
    }

    public static NodeExecutionResult awaitingHuman(String nodeId, String taskId) {
        return NodeExecutionResult.builder()
                .nodeId(nodeId)
                .status(ExecutionStatus.AWAITING_HUMAN)
                .humanTaskId(taskId)
                .outputChannels(new HashMap<>())
                .metadata(new HashMap<>())
                .build();
    }

    public static NodeExecutionResult cancelled(String nodeId) {
        return NodeExecutionResult.builder()
                .nodeId(nodeId)
                .status(ExecutionStatus.CANCELLED)
                .outputChannels(new HashMap<>())
                .metadata(new HashMap<>())
                .build();
    }

    public static NodeExecutionResult aborted(String nodeId, ErrorPayload error) {
        return NodeExecutionResult.builder()
                .nodeId(nodeId)
                .status(ExecutionStatus.ABORTED)
                .error(error)
                .outputChannels(new HashMap<>())
                .metadata(new HashMap<>())
                .build();
    }

    public boolean isSuccess() {
        return status == ExecutionStatus.SUCCESS;
    }

    public boolean isError() {
        return status == ExecutionStatus.ERROR;
    }

    public boolean isBlocked() {
        return status == ExecutionStatus.BLOCKED;
    }

    public boolean isCancelled() {
        return status == ExecutionStatus.CANCELLED;
    }

    public boolean isAwaitingHuman() {
        return status == ExecutionStatus.AWAITING_HUMAN;
    }
}
