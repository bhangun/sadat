package tech.kayys.wayang.node.core.exception;

/**
 * Exception thrown when node factory operations fail.
 */
public class NodeFactoryException extends NodeException {
    
    public NodeFactoryException(String message) {
        super(message, "NODE_FACTORY_ERROR", false);
    }
    
    public NodeFactoryException(String message, Throwable cause) {
        super(message, cause, "NODE_FACTORY_ERROR", false);
    }
    
    /**
     * Create exception for missing factory
     */
    public static NodeFactoryException noFactory(String implementationType) {
        return new NodeFactoryException(
            "No factory found for implementation type: " + implementationType
        );
    }
    
    /**
     * Create exception for invalid descriptor
     */
    public static NodeFactoryException invalidDescriptor(String reason) {
        return new NodeFactoryException(
            "Invalid node descriptor: " + reason
        );
    }
}