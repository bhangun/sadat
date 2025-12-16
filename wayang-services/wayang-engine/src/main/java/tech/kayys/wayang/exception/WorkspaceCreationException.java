package tech.kayys.wayang.exception;

import jakarta.ws.rs.core.Response;

/**
 * Workspace creation failed exception
 */
public class WorkspaceCreationException extends DesignerException {

    public WorkspaceCreationException(String message, Throwable cause) {
        super("WORKSPACE_CREATION_FAILED", message, cause);
    }

    @Override
    public Response.Status getHttpStatus() {
        return Response.Status.BAD_REQUEST;
    }
}
