package tech.kayys.wayang.sdk.dto;

import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;




/**
 * Response containing workflow run information
 */
public record WorkflowRunResponse(
    String runId,
    String workflowId,
    String workflowVersion,
    RunStatus status,
    String phase,
    Instant createdAt,
    Instant startedAt,
    Instant completedAt,
    Long durationMs,
    Integer nodesExecuted,
    Integer nodesTotal,
    Integer attemptNumber,
    Integer maxAttempts,
    String errorMessage,
    Map<String, Object> outputs
) {
    public boolean isRunning() {
        return status == RunStatus.RUNNING;
    }

    public boolean isWaiting() {
        return status == RunStatus.PENDING || status == RunStatus.SUSPENDED; // Mapped WAITING to PENDING/SUSPENDED based on RunStatus
    }

    public boolean isCompleted() {
        return status == RunStatus.SUCCEEDED;
    }

    public boolean isFailed() {
        return status == RunStatus.FAILED;
    }

    public boolean isTerminal() {
        return status == RunStatus.SUCCEEDED
            || status == RunStatus.FAILED
            || status == RunStatus.CANCELLED
            || status == RunStatus.TIMED_OUT;
    }
}
