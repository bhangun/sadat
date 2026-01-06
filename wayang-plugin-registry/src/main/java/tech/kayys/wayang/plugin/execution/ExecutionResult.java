package tech.kayys.wayang.plugin.execution;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Execution Result - Response from executor
 */
public class ExecutionResult {
    public String executionId;
    public ExecutionStatus status;
    public Map<String, Object> outputs = new HashMap<>();
    public ExecutionError error;
    public ExecutionMetrics metrics;
    public Instant completedAt;
}