package tech.kayys.wayang.common.contract;

import tech.kayys.wayang.common.domain.*;
import io.smallrye.mutiny.Uni;

/**
 * Core contract for all node types.
 * All nodes MUST implement this interface.
 * Supports both blocking and reactive execution.
 */
public interface Node {
    
    /**
     * Load node with descriptor and configuration
     */
    void onLoad(NodeDescriptor descriptor, Map<String, Object> config) 
        throws NodeException;
    
    /**
     * Execute node logic (reactive)
     * @return Uni<ExecutionResult> with success or error output
     */
    Uni<ExecutionResult> execute(NodeContext ctx);
    
    /**
     * Cleanup resources
     */
    void onUnload();
    
    /**
     * Node metadata
     */
    NodeDescriptor getDescriptor();
}
