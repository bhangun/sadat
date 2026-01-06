package tech.kayys.wayang.workflow.kernel;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * 🔒 Structured execution result.
 * Error as data, not exceptions.
 */
public interface NodeExecutionResult {

    NodeExecutionStatus getStatus();

    String getNodeId();

    Instant getExecutedAt();

    Duration getDuration();

    // Updated context (if any)
    ExecutionContext getUpdatedContext();

    // Error details (if status == FAILURE)
    ExecutionError getError();

    // Wait details (if status == WAIT)
    WaitInfo getWaitInfo();

    // Execution metadata
    Map<String, Object> getMetadata();
}
