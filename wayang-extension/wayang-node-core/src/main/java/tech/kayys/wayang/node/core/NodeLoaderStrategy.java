package tech.kayys.wayang.node.core.loader;

import tech.kayys.wayang.node.core.Node;
import tech.kayys.wayang.node.core.exception.NodeLoadException;
import tech.kayys.wayang.node.core.model.NodeDescriptor;
import tech.kayys.wayang.node.core.model.SandboxLevel;

/**
 * Strategy interface for loading nodes with different isolation levels.
 * 
 * Each strategy handles a specific sandbox level and provides
 * appropriate isolation guarantees.
 */
public interface NodeLoaderStrategy {
    
    /**
     * Load a node with the specified isolation level.
     * 
     * @param descriptor The node descriptor
     * @param nodeInstance The node instance to load
     * @return A wrapped node with isolation applied
     * @throws NodeLoadException if loading fails
     */
    Node load(NodeDescriptor descriptor, Node nodeInstance) throws NodeLoadException;
    
    /**
     * Unload a node and clean up resources.
     * 
     * @param descriptor The node descriptor
     * @param node The node to unload
     */
    void unload(NodeDescriptor descriptor, Node node);
    
    /**
     * Check if this strategy supports the given sandbox level.
     * 
     * @param sandboxLevel The sandbox level to check
     * @return true if supported
     */
    boolean supports(SandboxLevel sandboxLevel);
    
    /**
     * Get the sandbox level this strategy handles.
     * 
     * @return The sandbox level
     */
    SandboxLevel getSandboxLevel();
}