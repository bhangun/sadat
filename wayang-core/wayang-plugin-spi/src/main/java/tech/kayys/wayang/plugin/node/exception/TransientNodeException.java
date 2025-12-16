package tech.kayys.wayang.plugin.node.exception;

/**
 * Exception indicating a transient failure during node execution.
 *
 * <p>
 * Transient errors are conditions that are expected to resolve automatically
 * with time. The workflow engine is allowed and encouraged to retry executions
 * that fail with this exception.
 *
 * <p>
 * Common scenarios include:
 * <ul>
 * <li>External API timeouts</li>
 * <li>Rate limiting (HTTP 429)</li>
 * <li>Network connectivity issues</li>
 * <li>Temporary unavailability of dependent services</li>
 * </ul>
 *
 * <p>
 * Retries should normally involve exponential backoff and jitter.
 */
public class TransientNodeException extends NodeException {

    /**
     * Creates a new retryable exception with a message.
     *
     * @param message the detail message
     */
    public TransientNodeException(String message) {
        super(message);
    }

    /**
     * Creates a new retryable exception with a message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause
     */
    public TransientNodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
