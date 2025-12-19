package tech.kayys.wayang.exception;

import jakarta.ws.rs.core.Response;
import java.util.Map;

public class WorkflowDeletionException extends DesignerException {
    public WorkflowDeletionException(String message) {
        super("WORKFLOW_DELETION_FAILED", message);
    }

    @Override
    public Response.Status getHttpStatus() {
        return Response.Status.BAD_REQUEST;
    }
}
