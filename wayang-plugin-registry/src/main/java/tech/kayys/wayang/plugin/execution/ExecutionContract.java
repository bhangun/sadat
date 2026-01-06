package tech.kayys.wayang.plugin.execution;

import java.time.Instant;
import java.util.Map;

import tech.kayys.wayang.plugin.TraceContext;
import tech.kayys.wayang.plugin.executor.ExecutorDescriptor;
import tech.kayys.wayang.plugin.node.NodeDescriptor;

/**
 * Execution Contract - Language-neutral contract between Engine and Executor
 * This is THE critical artifact that enables:
 * - Replay
 * - Audit
 * - Retry
 * - Time-travel
 * - Multi-language executors
 */
public class ExecutionContract {
    // Identity
    public String executionId;
    public String workflowRunId;

    // Node information
    public NodeDescriptor node;

    // Executor information
    public ExecutorDescriptor executor;

    // Execution mode
    public ExecutionMode mode;

    // Data
    public Map<String, Object> inputs;
    public Map<String, Object> config;
    public ExecutionContext context;

    // Observability
    public TraceContext trace;

    // Timing
    public Instant createdAt;
    public Instant expiresAt;
}
