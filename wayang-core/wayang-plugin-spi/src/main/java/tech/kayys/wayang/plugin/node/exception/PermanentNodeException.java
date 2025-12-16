package tech.kayys.wayang.plugin.node.exception;

/**
 * Exception indicating a permanent failure during node execution.
 *
 * <p>
 * Permanent errors must <strong>not</strong> be retried, as retrying will
 * not resolve the problem. These failures usually indicate:
 * <ul>
 * <li>Invalid input or missing required fields</li>
 * <li>Misconfiguration of the node</li>
 * <li>Unsupported operation</li>
 * <li>Security or sandbox restriction violations</li>
 * <li>Business logic violations</li>
 * </ul>
 *
 * <p>
 * When thrown, the workflow engine should mark the node as failed and
 * escalate control to the workflow's error handler.
 */
public class PermanentNodeException extends NodeException {

    /**
     * Creates a new permanent failure exception with a message.
     *
     * @param message the detail message
     */
    public PermanentNodeException(String message) {
        super(message);
    }

    /**
     * Creates a new permanent failure exception with a message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause
     */
    public PermanentNodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
