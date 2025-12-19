package tech.kayys.wayang.sdk.dto;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;




/**
 * Execution options for workflow runs
 */
public record ExecutionOptions(
    Priority priority,
    long timeoutMs,
    boolean dryRun,
    String parentRunId,
    Map<String, String> tags
) {
    public enum Priority {
        LOW, NORMAL, HIGH, CRITICAL
    }

    public static ExecutionOptions defaults() {
        return new ExecutionOptions(
            Priority.NORMAL,
            3600000L, // 1 hour
            false,
            null,
            Map.of()
        );
    }

    public ExecutionOptions withPriority(Priority priority) {
        return new ExecutionOptions(priority, timeoutMs, dryRun, parentRunId, tags);
    }

    public ExecutionOptions withTimeout(long timeoutMs) {
        return new ExecutionOptions(priority, timeoutMs, dryRun, parentRunId, tags);
    }

    public ExecutionOptions asDryRun() {
        return new ExecutionOptions(priority, timeoutMs, true, parentRunId, tags);
    }
}
