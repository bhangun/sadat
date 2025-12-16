package tech.kayys.wayang.exception;

import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

/**
 * Workflow not found exception
 */
public class WorkflowNotFoundException extends DesignerException {

    public WorkflowNotFoundException(UUID workflowId, String tenantId) {
        super(
                "WORKFLOW_NOT_FOUND",
                String.format("Workflow %s not found for tenant %s", workflowId, tenantId),
                null,
                Map.of("workflowId", workflowId, "tenantId", tenantId));
    }

    @Override
    public Response.Status getHttpStatus() {
        return Response.Status.NOT_FOUND;
    }
}