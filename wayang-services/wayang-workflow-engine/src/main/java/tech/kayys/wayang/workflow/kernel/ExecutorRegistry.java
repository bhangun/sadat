package tech.kayys.wayang.workflow.kernel;

import java.util.List;

import io.smallrye.mutiny.Uni;

/**
 * 🔒 Executor registry for discovery
 */
public interface ExecutorRegistry {

    /**
     * Discover executors for a node type
     */
    Uni<List<ExecutorInfo>> discoverExecutors(String nodeType);

    /**
     * Register an executor
     */
    Uni<Void> registerExecutor(ExecutorInfo executor);

    /**
     * Heartbeat from executor
     */
    Uni<Void> heartbeat(String executorId);

    /**
     * Get execution endpoint for an executor
     */
    Uni<String> getExecutionEndpoint(String executorId);
}