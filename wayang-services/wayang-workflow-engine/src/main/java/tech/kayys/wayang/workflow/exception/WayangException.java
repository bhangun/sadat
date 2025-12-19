package tech.kayys.wayang.workflow.exception;

/**
 * Base exception for Wayang platform
 */
public abstract class WayangException extends RuntimeException {
    private final String errorCode;

    public WayangException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public WayangException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}