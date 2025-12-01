package tech.kayys.wayang.node.core.factory;

import tech.kayys.wayang.node.core.Node;
import tech.kayys.wayang.node.core.model.NodeDescriptor;
import tech.kayys.wayang.node.core.exception.NodeFactoryException;

/**
 * Factory interface for creating node instances.
 * 
 * Implementations handle different node types and isolation strategies.
 */
public interface NodeFactory {
    /**
     * Create a node instance from a descriptor.
     * 
     * @param descriptor The node descriptor
     * @return A new node instance
     * @throws NodeFactoryException if creation fails
     */
    Node create(NodeDescriptor descriptor) throws NodeFactoryException;
    
    /**
     * Validate a node descriptor before creation.
     * 
     * @param descriptor The descriptor to validate
     * @throws NodeFactoryException if validation fails
     */
    void validate(NodeDescriptor descriptor) throws NodeFactoryException;
    
    /**
     * Check if this factory supports the given descriptor.
     * 
     * @param descriptor The descriptor to check
     * @return true if this factory can create nodes from this descriptor
     */
    boolean supports(NodeDescriptor descriptor);
    
    /**
     * Get the implementation type this factory handles.
     * 
     * @return The implementation type
     */
    tech.kayys.wayang.node.core.model.ImplementationType getImplementationType();
}