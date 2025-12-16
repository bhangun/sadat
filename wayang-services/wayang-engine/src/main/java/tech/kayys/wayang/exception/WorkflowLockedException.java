package tech.kayys.wayang.exception;

import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

/**
 * Workflow locked by another user exception
 */
public class WorkflowLockedException extends DesignerException {

    public WorkflowLockedException(UUID workflowId, String message) {
        super(
                "WORKFLOW_LOCKED",
                String.format("Workflow %s is locked: %s", workflowId, message),
                null,
                Map.of("workflowId", workflowId));
    }

    @Override
    public Response.Status getHttpStatus() {
        return Response.Status.CONFLICT;
    }
}