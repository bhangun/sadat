package tech.kayys.wayang.workflow.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.schema.node.NodeDefinition;
import tech.kayys.wayang.workflow.executor.NodeExecutionResult;
import tech.kayys.wayang.workflow.executor.NodeExecutor;
import tech.kayys.wayang.workflow.executor.NodeExecutorRegistry;
import tech.kayys.wayang.schema.execution.ExecutionConfig;

/**
 * DelegatingNodeExecutor - Delegates node execution to the appropriate executor
 * with configurable execution patterns
 *
 * This executor acts as a dispatcher that routes node execution to the
 * appropriate NodeExecutor based on the node type and configuration.
 */
@ApplicationScoped
public class DelegatingNodeExecutor implements NodeExecutor {

    @Inject
    NodeExecutorRegistry registry;

    @Override
    public Uni<NodeExecutionResult> execute(NodeDefinition nodeDef, NodeContext context) {
        ExecutionConfig executionConfig = nodeDef.getExecution();

        if (executionConfig != null) {
            // Handle different execution modes based on configuration
            String mode = executionConfig.getMode();
            if (mode != null) {
                switch (mode.toLowerCase()) {
                    case "async":
                        return executeAsynchronously(nodeDef, context);
                    case "stream":
                        return executeWithStreaming(nodeDef, context);
                    case "sync":
                    default:
                        return executeSynchronously(nodeDef, context);
                }
            }
        }

        // Default to synchronous execution by delegating to the appropriate executor
        return executeSynchronously(nodeDef, context);
    }

    /**
     * Execute node synchronously (default behavior)
     */
    private Uni<NodeExecutionResult> executeSynchronously(NodeDefinition nodeDef, NodeContext context) {
        return registry.getExecutor(nodeDef.getType()).execute(nodeDef, context);
    }

    /**
     * Execute node asynchronously
     */
    private Uni<NodeExecutionResult> executeAsynchronously(NodeDefinition nodeDef, NodeContext context) {
        // For async execution, we might want to return immediately and execute in
        // background
        // This would typically involve queuing the work and returning a pending result
        return Uni.createFrom().deferred(() -> executeSynchronously(nodeDef, context));
    }

    /**
     * Execute node with streaming
     */
    private Uni<NodeExecutionResult> executeWithStreaming(NodeDefinition nodeDef, NodeContext context) {
        // For streaming execution, we might handle chunked data
        // This would typically involve processing data in chunks and returning results
        // incrementally
        return executeSynchronously(nodeDef, context);
    }

    @Override
    public String getNodeType() {
        // This is a delegating executor, so it doesn't have a specific node type
        return "DELEGATING";
    }

    @Override
    public boolean canHandle(String nodeType) {
        // Can handle any node type since it delegates to the appropriate executor
        return true;
    }
}
