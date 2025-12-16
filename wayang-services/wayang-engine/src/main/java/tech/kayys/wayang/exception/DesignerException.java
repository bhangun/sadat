package tech.kayys.wayang.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Base exception for all designer exceptions
 */
public abstract class DesignerException extends RuntimeException {
    private final String code;
    private final Map<String, Object> metadata;

    public DesignerException(String code, String message) {
        this(code, message, null, null);
    }

    public DesignerException(String code, String message, Throwable cause) {
        this(code, message, cause, null);
    }

    public DesignerException(String code, String message, Throwable cause,
            Map<String, Object> metadata) {
        super(message, cause);
        this.code = code;
        this.metadata = metadata != null ? metadata : Map.of();
    }

    public String getCode() {
        return code;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public abstract Response.Status getHttpStatus();
}