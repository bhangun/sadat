package tech.kayys.wayang.workflow.model;

import java.util.Map;

public record NodeExecutionState(
        String nodeId,
        NodeStatus status,
        Map<String, Object> outputs,
        String errorMessage,
        int attemptCount) {
    public enum NodeStatus {
        PENDING,
        RUNNING,
        SUCCEEDED,
        FAILED,
        SKIPPED
    }
}
