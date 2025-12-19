package tech.kayys.wayang.exception;

import jakarta.ws.rs.core.Response;

public class InternalException extends DesignerException {
    public InternalException(String message, Throwable cause) {
        super("INTERNAL_ERROR", message, cause);
    }

    @Override
    public Response.Status getHttpStatus() {
        return Response.Status.INTERNAL_SERVER_ERROR;
    }
}
