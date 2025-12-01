package tech.kayys.wayang.node.core.exception;

/**
 * Base exception for all node-related errors.
 * 
 * Provides structured error information for proper handling
 * and reporting across the platform.
 */
public class NodeException extends Exception {
    
    private final String errorCode;
    private final boolean retryable;
    
    public NodeException(String message) {
        this(message, null, "NODE_ERROR", false);
    }
    
    public NodeException(String message, Throwable cause) {
        this(message, cause, "NODE_ERROR", false);
    }
    
    public NodeException(String message, String errorCode, boolean retryable) {
        this(message, null, errorCode, retryable);
    }
    
    public NodeException(String message, Throwable cause, String errorCode, boolean retryable) {
        super(message, cause);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public boolean isRetryable() {
        return retryable;
    }
    
    /**
     * Create a retryable exception
     */
    public static NodeException retryable(String message, Throwable cause) {
        return new NodeException(message, cause, "NODE_ERROR_RETRYABLE", true);
    }
    
    /**
     * Create a non-retryable exception
     */
    public static NodeException fatal(String message, Throwable cause) {
        return new NodeException(message, cause, "NODE_ERROR_FATAL", false);
    }
}