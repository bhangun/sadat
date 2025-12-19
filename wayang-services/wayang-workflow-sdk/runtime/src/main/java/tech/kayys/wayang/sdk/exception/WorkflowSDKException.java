package tech.kayys.wayang.sdk.exception;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;




/**
 * Base exception for SDK operations
 */
public class WorkflowSDKException extends RuntimeException {
    private final String errorCode;
    private final Map<String, Object> details;

    public WorkflowSDKException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.details = Map.of();
    }

    public WorkflowSDKException(String message, String errorCode, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public WorkflowSDKException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = Map.of();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
