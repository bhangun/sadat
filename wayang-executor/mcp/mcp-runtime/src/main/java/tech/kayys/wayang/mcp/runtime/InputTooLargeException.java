package tech.kayys.wayang.mcp.runtime;

/**
 * Exception thrown when the input size exceeds the limit.
 */
public class InputTooLargeException extends RuntimeException {
    public InputTooLargeException(String message) {
        super(message);
    }
}
