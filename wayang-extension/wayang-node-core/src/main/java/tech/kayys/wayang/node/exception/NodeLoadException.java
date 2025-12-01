package tech.kayys.wayang.node.core.exception;

/**
 * Exception thrown when node loading fails.
 */
public class NodeLoadException extends NodeException {
    
    public NodeLoadException(String message) {
        super(message, "NODE_LOAD_ERROR", false);
    }
    
    public NodeLoadException(String message, Throwable cause) {
        super(message, cause, "NODE_LOAD_ERROR", false);
    }
    
    /**
     * Create exception for unsupported sandbox level
     */
    public static NodeLoadException unsupportedSandboxLevel(String level) {
        return new NodeLoadException(
            "Unsupported sandbox level: " + level
        );
    }
    
    /**
     * Create exception for artifact resolution failure
     */
    public static NodeLoadException artifactResolutionFailed(String coordinate, Throwable cause) {
        return new NodeLoadException(
            "Failed to resolve artifact: " + coordinate,
            cause
        );
    }
}