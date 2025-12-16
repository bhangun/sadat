package tech.kayys.wayang.exception;

import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

/**
 * Workflow validation failed exception
 */
public class WorkflowValidationException extends DesignerException {

    public WorkflowValidationException(UUID workflowId, String reason) {
        super(
                "WORKFLOW_VALIDATION_FAILED",
                String.format("Workflow %s validation failed: %s", workflowId, reason),
                null,
                Map.of("workflowId", workflowId, "reason", reason));
    }

    @Override
    public Response.Status getHttpStatus() {
        return Response.Status.BAD_REQUEST;
    }
}
