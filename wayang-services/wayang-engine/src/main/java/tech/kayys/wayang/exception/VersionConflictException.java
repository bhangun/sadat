package tech.kayys.wayang.exception;

import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

/**
 * Version conflict exception (optimistic locking)
 */
public class VersionConflictException extends DesignerException {

    public VersionConflictException(UUID workflowId, Long expectedVersion, Long actualVersion) {
        super(
                "VERSION_CONFLICT",
                String.format("Version conflict for workflow %s: expected %d, actual %d",
                        workflowId, expectedVersion, actualVersion),
                null,
                Map.of(
                        "workflowId", workflowId,
                        "expectedVersion", expectedVersion,
                        "actualVersion", actualVersion));
    }

    @Override
    public Response.Status getHttpStatus() {
        return Response.Status.CONFLICT;
    }
}