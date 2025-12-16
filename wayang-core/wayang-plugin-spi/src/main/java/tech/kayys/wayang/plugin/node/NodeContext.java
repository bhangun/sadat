package tech.kayys.wayang.plugin.node;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import io.micrometer.core.instrument.MeterRegistry;
import tech.kayys.wayang.plugin.SandboxLevel;
import tech.kayys.wayang.plugin.node.obervability.Context;
import tech.kayys.wayang.plugin.node.obervability.Span;
import tech.kayys.wayang.plugin.node.obervability.Tracer;

/**
 * Node execution context - isolated, thread-safe, tenant-aware.
 * Immutable except for allowed variable/output mutations.
 */
public interface NodeContext {

    // ------------------------------------------------------------
    // 1. Execution Metadata
    // ------------------------------------------------------------

    String getRunId(); // unique per workflow-run

    String getNodeId(); // unique per node-instance in graph

    String getTenantId(); // multi-tenant isolation

    String getTraceId(); // tracing root

    Optional<String> getSpanId(); // optional child span for nested ops

    // Execution timing
    Instant getStartTime();

    // ------------------------------------------------------------
    // 2. Inputs / Outputs
    // ------------------------------------------------------------

    <T> T getInput(String portName);

    Map<String, Object> getAllInputs();

    void setOutput(String portName, Object value);

    // Streaming output (advanced, optional)
    default void emitOutput(String portName, Object chunk) {
        throw new UnsupportedOperationException("Streaming not supported");
    }

    // ------------------------------------------------------------
    // 3. Workflow Variables (Shared State)
    // ------------------------------------------------------------

    <T> T getVariable(String name);

    void setVariable(String name, Object value);

    Map<String, Object> getAllVariables(); // readonly view

    // ------------------------------------------------------------
    // 4. Metadata / Contextual Data
    // ------------------------------------------------------------

    Map<String, Object> getMetadata(); // headers, retry count, tags

    // Cancellation token (cooperative)
    CancellationToken getCancellationToken();

    // ------------------------------------------------------------
    // 5. Observability, Logs, Metrics, Tracing
    // ------------------------------------------------------------

    Logger getLogger(); // structured logger

    void emitEvent(String eventType, Map<String, Object> payload);

    Tracer getTracer(); // for OTel spans

    MeterRegistry getMeterRegistry(); // micrometer

    // Convenience method
    default Span newSpan(String name) {
        return getTracer().spanBuilder(name)
                .setParent(Context.current())
                .startSpan();
    }

    // ------------------------------------------------------------
    // 6. Security & Sandbox
    // ------------------------------------------------------------

    SandboxLevel getSandboxLevel();

    PermissionSet getPermissions(); // e.g., FS read, network allowlist

    // ------------------------------------------------------------
    // 7. Resource Budgeting
    // ------------------------------------------------------------

    ResourceBudget getResourceBudget(); // CPU, mem, tokens, I/O

    void consumeCpu(long microSeconds);

    void consumeTokens(long tokens); // for LLM/token accounting

    // ------------------------------------------------------------
    // 8. Child Contexts (Parallelism / Iteration)
    // ------------------------------------------------------------

    NodeContext createChild(); // lightweight fork

    NodeContext createChildWithMetadata(Map<String, Object> extra);

    // ------------------------------------------------------------
    // 9. Checkpoint-friendly state
    // ------------------------------------------------------------

    Map<String, Object> exportCheckpointState();

    void importCheckpointState(Map<String, Object> state);

    // ------------------------------------------------------------
    // 10. Runtime Utilities
    // ------------------------------------------------------------

    ScheduledExecutorService executor(); // non-blocking ops

    String getExecutionMode(); // sync/async/streaming

    default boolean isStreaming() {
        return "streaming".equalsIgnoreCase(getExecutionMode());
    }
}
