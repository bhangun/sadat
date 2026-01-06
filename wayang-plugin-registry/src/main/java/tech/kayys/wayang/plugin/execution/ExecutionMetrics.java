package tech.kayys.wayang.plugin.execution;

import java.util.HashMap;
import java.util.Map;

/**
 * Execution Metrics - Performance and resource metrics
 */
public class ExecutionMetrics {
    public long durationMs;
    public long tokens; // For LLM executors
    public long bytesProcessed; // For data executors
    public int retryCount;
    public Map<String, Object> custom = new HashMap<>();
}
