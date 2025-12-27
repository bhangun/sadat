package tech.kayys.wayang.workflow.executor;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.schema.node.NodeDefinition;
import tech.kayys.wayang.workflow.service.NodeContext;

/**
 * NodeExecutor - Interface for executing workflow nodes in a use case agnostic
 * way
 *
 * This interface provides a generic contract for node execution that can be
 * implemented
 * by various node types without requiring knowledge of specific business logic.
 */
public interface NodeExecutor {
    /**
     * Execute a node with the given context
     * 
     * @param nodeDef The node definition containing configuration
     * @param context The execution context with inputs and metadata
     * @return A Uni containing the execution result
     */
    Uni<NodeExecutionResult> execute(NodeDefinition nodeDef, NodeContext context);

    /**
     * Get the node type this executor handles
     * 
     * @return The node type identifier
     */
    default String getNodeType() {
        // Default implementation - can be overridden by specific executors
        return this.getClass().getSimpleName().replace("NodeExecutor", "").toLowerCase();
    }

    /**
     * Check if this executor can handle a specific node type
     * 
     * @param nodeType The node type to check
     * @return true if this executor can handle the node type
     */
    default boolean canHandle(String nodeType) {
        return getNodeType().equals(nodeType);
    }
}
