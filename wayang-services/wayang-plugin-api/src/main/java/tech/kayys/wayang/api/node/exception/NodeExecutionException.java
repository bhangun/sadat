package tech.kayys.wayang.api.node.exception;

/**
 * Generic exception raised during node execution.
 *
 * <p>
 * This type represents execution errors that cannot be strictly classified
 * as transient or permanent by the node implementation. The workflow engine may
 * apply policy or heuristics to determine retry behavior.
 *
 * <p>
 * This is typically used when:
 * <ul>
 * <li>Unexpected unhandled exceptions occur in user code</li>
 * <li>An internal invariant is violated</li>
 * <li>A general execution failure occurs that needs logging and escalation</li>
 * </ul>
 *
 * <p>
 * Workflow engines usually treat this as a serious failure unless configured
 * otherwise.
 */
public class NodeExecutionException extends NodeException {

    /**
     * Creates a new generic execution exception with a message.
     *
     * @param message the detail message
     */
    public NodeExecutionException(String message) {
        super(message);
    }

    /**
     * Creates a new generic execution exception with a message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause
     */
    public NodeExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    private static final long serialVersionUID = 1L;

    public NodeExecutionException() {
        super();
    }

    public NodeExecutionException(Throwable cause) {
        super(cause);
    }
}