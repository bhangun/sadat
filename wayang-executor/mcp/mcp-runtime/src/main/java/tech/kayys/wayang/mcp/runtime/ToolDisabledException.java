package tech.kayys.wayang.mcp.runtime;

/**
 * Exception thrown when a tool is disabled.
 */
public class ToolDisabledException extends RuntimeException {
    public ToolDisabledException(String message) {
        super(message);
    }
}
