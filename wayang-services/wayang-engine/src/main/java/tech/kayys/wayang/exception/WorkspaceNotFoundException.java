package tech.kayys.wayang.exception;

import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

/**
 * Workspace not found exception
 */
public class WorkspaceNotFoundException extends DesignerException {

    public WorkspaceNotFoundException(UUID workspaceId, String tenantId) {
        super(
                "WORKSPACE_NOT_FOUND",
                String.format("Workspace %s not found for tenant %s", workspaceId, tenantId),
                null,
                Map.of("workspaceId", workspaceId, "tenantId", tenantId));
    }

    @Override
    public Response.Status getHttpStatus() {
        return Response.Status.NOT_FOUND;
    }
}