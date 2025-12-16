package tech.kayys.wayang.exception;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Global exception mapper
 */
@Provider
public class DesignerExceptionMapper {

    private static final Logger LOG = Logger.getLogger(DesignerExceptionMapper.class);

    /**
     * Handle DesignerException and subclasses
     */
    @ServerExceptionMapper
    public RestResponse<ErrorResponse> mapDesignerException(DesignerException ex) {
        LOG.warnf(ex, "Designer exception: %s", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                ex.getCode(),
                ex.getMessage(),
                ex.getMetadata());

        return RestResponse.status(ex.getHttpStatus(), error);
    }

    /**
     * Handle generic exceptions
     */
    @ServerExceptionMapper
    public RestResponse<ErrorResponse> mapGenericException(Exception ex) {
        LOG.errorf(ex, "Unexpected exception: %s", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                Map.of());

        return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, error);
    }

    /**
     * Handle validation exceptions
     */
    @ServerExceptionMapper
    public RestResponse<ErrorResponse> mapValidationException(
            jakarta.validation.ValidationException ex) {
        LOG.warnf("Validation exception: %s", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                "VALIDATION_ERROR",
                ex.getMessage(),
                Map.of());

        return RestResponse.status(Response.Status.BAD_REQUEST, error);
    }

    /**
     * Error response DTO
     */
    public record ErrorResponse(
            String code,
            String message,
            Map<String, Object> metadata,
            String timestamp,
            String traceId) {
        public ErrorResponse(String code, String message, Map<String, Object> metadata) {
            this(
                    code,
                    message,
                    metadata,
                    Instant.now().toString(),
                    UUID.randomUUID().toString());
        }
    }
}
