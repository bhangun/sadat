package tech.kayys.wayang.node.core.exception;

/**
 * Exception thrown when isolation policies are violated.
 */
public class IsolationException extends NodeException {
    
    public IsolationException(String message) {
        super(message, "ISOLATION_ERROR", false);
    }
    
    public IsolationException(String message, Throwable cause) {
        super(message, cause, "ISOLATION_ERROR", false);
    }
    
    /**
     * Create exception for capability violation
     */
    public static IsolationException capabilityViolation(String capability, String sandboxLevel) {
        return new IsolationException(
            String.format("Capability '%s' not allowed for sandbox level: %s", 
                capability, sandboxLevel)
        );
    }
}