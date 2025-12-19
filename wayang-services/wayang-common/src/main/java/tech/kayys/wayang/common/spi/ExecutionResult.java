package tech.kayys.wayang.common.spi;

import tech.kayys.wayang.schema.execution.ErrorPayload;
import java.util.Map;
import java.util.Collections;

public record ExecutionResult(
    Status status,
    Object data,
    String outputChannel,
    ErrorPayload error,
    String blockedReason,
    Map<String, Object> metadata
) {
    public enum Status {
        SUCCESS, ERROR, BLOCKED, WAITING
    }

    public boolean isSuccess() { return status == Status.SUCCESS; }
    public boolean isError() { return status == Status.ERROR; }
    public boolean isBlocked() { return status == Status.BLOCKED; }

    public static ExecutionResult success(Object data) {
        return new ExecutionResult(Status.SUCCESS, data, "default", null, null, Collections.emptyMap());
    }

    public static ExecutionResult success(Object data, String channel) {
        return new ExecutionResult(Status.SUCCESS, data, channel, null, null, Collections.emptyMap());
    }

    public static ExecutionResult blocked(String reason) {
        return new ExecutionResult(Status.BLOCKED, null, null, null, reason, Collections.emptyMap());
    }

    public static ExecutionResult error(ErrorPayload error) {
        return new ExecutionResult(Status.ERROR, null, null, error, null, Collections.emptyMap());
    }
}
