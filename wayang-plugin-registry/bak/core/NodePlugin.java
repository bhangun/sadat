
/**
 * Node plugin - provides custom node types
 */
public interface NodePlugin extends Plugin {
    /**
     * Get provided node factories
     */
    Map<String, NodeFactory> getNodeFactories();
    
    /**
     * Get node descriptors
     */
    List<NodeDescriptor> getNodeDescriptors();
}