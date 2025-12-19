package tech.kayys.wayang.common.spi;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.schema.node.NodeDefinition;

/**
 * Standard interface for all executable nodes in Wayang.
 */
public interface Node {
    
    /**
     * Called when the node is loaded into memory.
     */
    Uni<Void> onLoad(NodeDefinition descriptor, NodeConfig config);

    /**
     * Executes the node logic.
     */
    Uni<ExecutionResult> execute(NodeContext context);

    /**
     * Returns true if the node is stateless.
     */
    boolean isStateless();

    /**
     * Called when the node is being unloaded.
     */
    Uni<Void> onUnload();
}
