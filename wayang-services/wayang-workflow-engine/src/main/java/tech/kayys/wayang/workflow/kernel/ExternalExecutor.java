package tech.kayys.wayang.workflow.kernel;

import java.util.Set;

import io.smallrye.mutiny.Uni;

/**
 * 🔒 Contract for external executors
 */
public interface ExternalExecutor {

    /**
     * Unique identifier for this executor
     */
    String getExecutorId();

    /**
     * Node types this executor can handle
     */
    Set<String> getSupportedNodeTypes();

    /**
     * Health check for load balancing
     */
    Uni<ExecutorHealth> healthCheck();

    /**
     * Maximum concurrent executions
     */
    int getMaxConcurrency();
}
