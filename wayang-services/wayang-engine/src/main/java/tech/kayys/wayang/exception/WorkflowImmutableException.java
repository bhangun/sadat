package tech.kayys.wayang.exception;

import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

/**
 * Workflow is immutable (published) exception
 */
public class WorkflowImmutableException extends DesignerException {

    public WorkflowImmutableException(UUID workflowId, String reason) {
        super(
                "WORKFLOW_IMMUTABLE",
                String.format("Workflow %s cannot be modified: %s", workflowId, reason),
                null,
                Map.of("workflowId", workflowId, "reason", reason));
    }

    @Override
    public Response.Status getHttpStatus() {
        return Response.Status.CONFLICT;
    }
}
