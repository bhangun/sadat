package tech.kayys.wayang.plugin;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Execution result with success/error outputs
 */
public class ExecutionResult {

    private final Status status;
    private final Map<String, Object> outputs;
    private final List<String> logs;
    private final Optional<ErrorPayload> error;
    private final Map<String, Object> metadata;

    public enum Status {
        SUCCESS,
        FAILED,
        BLOCKED,
        TIMEOUT,
        RETRY
    }

    public ExecutionResult(Status status, Map<String, Object> outputs, List<String> logs, Optional<ErrorPayload> error,
            Map<String, Object> metadata) {
        this.status = status;
        this.outputs = outputs;
        this.logs = logs;
        this.error = error;
        this.metadata = metadata;
    }

    public static ExecutionResult success(Map<String, Object> outputs) {
        return new ExecutionResult(Status.SUCCESS, outputs, List.of(), Optional.empty(), Map.of());
    }

    public static ExecutionResult failed(String reason) {
        var error = ErrorPayload.builder()
                .message(reason)
                .retryable(false)
                .build();
        return new ExecutionResult(Status.FAILED, Map.of(), List.of(), Optional.of(error), Map.of());
    }

    public static ExecutionResult error(ErrorPayload error) {
        return new ExecutionResult(Status.FAILED, Map.of(), List.of(), Optional.of(error), Map.of());
    }

    public static ExecutionResult blocked(String reason) {
        return new ExecutionResult(Status.BLOCKED, Map.of(), List.of(reason), Optional.empty(), Map.of());
    }

    // Getters
    public Status getStatus() {
        return status;
    }

    public Map<String, Object> getOutputs() {
        return outputs;
    }

    public Optional<ErrorPayload> getError() {
        return error;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public List<String> getLogs() {
        return logs;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Immutable-style "setters" that return a new ExecutionResult with the
     * requested change.
     */
    public ExecutionResult withStatus(Status status) {
        return new ExecutionResult(status, this.outputs, this.logs, this.error, this.metadata);
    }

    public ExecutionResult withOutputs(Map<String, Object> outputs) {
        return new ExecutionResult(this.status, outputs, this.logs, this.error, this.metadata);
    }

    public ExecutionResult withLogs(List<String> logs) {
        return new ExecutionResult(this.status, this.outputs, logs, this.error, this.metadata);
    }

    public ExecutionResult withError(Optional<ErrorPayload> error) {
        return new ExecutionResult(this.status, this.outputs, this.logs, error, this.metadata);
    }

    public ExecutionResult withError(ErrorPayload error) {
        return withError(Optional.ofNullable(error));
    }

    public ExecutionResult withMetadata(Map<String, Object> metadata) {
        return new ExecutionResult(this.status, this.outputs, this.logs, this.error, metadata);
    }
}
