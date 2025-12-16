package tech.kayys.wayang.plugin.node.exception;

/**
 * Base class for all node-related exceptions in the workflow runtime.
 *
 * <p>
 * This exception represents any failure that occurs during the lifecycle or
 * execution of a node. All node-specific exception types must extend this
 * class.
 *
 * <p>
 * The workflow engine may inspect subclasses of this type to determine
 * whether retries are allowed, whether the workflow should halt, or whether the
 * error should be escalated.
 */
public class NodeException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new NodeException with a descriptive message.
     *
     * @param message the detail message
     */
    public NodeException(String message) {
        super(message);
    }

    /**
     * Creates a new NodeException with a descriptive message and a cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause of the error
     */
    public NodeException(String message, Throwable cause) {
        super(message, cause);
    }

    public NodeException() {
        super();
    }

    public NodeException(Throwable cause) {
        super(cause);
    }
}