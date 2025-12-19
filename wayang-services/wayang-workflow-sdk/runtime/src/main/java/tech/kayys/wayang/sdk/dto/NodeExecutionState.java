package tech.kayys.wayang.sdk.dto;

import java.time.Instant;
import java.util.Map;

public record NodeExecutionState(
    String nodeId,
    NodeStatus status,
    Map<String, Object> inputs,
    Map<String, Object> outputs,
    String errorMessage,
    Instant startedAt,
    Instant completedAt
) {
    public enum NodeStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        SKIPPED,
        CANCELLED
    }
}
