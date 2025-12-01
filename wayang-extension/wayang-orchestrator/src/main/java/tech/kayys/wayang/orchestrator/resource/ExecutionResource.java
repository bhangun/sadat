package tech.kayys.wayang.orchestrator.resource;

import tech.kayys.wayang.common.domain.*;
import tech.kayys.wayang.orchestrator.service.ExecutionService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/execution")
@Tag(name = "Workflow Execution")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExecutionResource {
    
    @Inject
    ExecutionService executionService;
    
    @POST
    @Path("/start")
    public Uni<ExecutionRun> startExecution(
        @HeaderParam("X-Tenant-ID") String tenantId,
        @HeaderParam("X-User-ID") String userId,
        ExecutionRequest request
    ) {
        return executionService.startExecution(tenantId, userId, request);
    }
    
    @GET
    @Path("/{runId}")
    public Uni<ExecutionRun> getExecutionStatus(
        @HeaderParam("X-Tenant-ID") String tenantId,
        @PathParam("runId") String runId
    ) {
        return executionService.getExecutionRun(tenantId, runId);
    }
    
    @POST
    @Path("/{runId}/cancel")
    public Uni<Void> cancelExecution(
        @HeaderParam("X-Tenant-ID") String tenantId,
        @PathParam("runId") String runId
    ) {
        return executionService.cancelExecution(tenantId, runId);
    }
    
    @POST
    @Path("/{runId}/resume")
    public Uni<ExecutionRun> resumeExecution(
        @HeaderParam("X-Tenant-ID") String tenantId,
        @PathParam("runId") String runId,
        @QueryParam("checkpointId") String checkpointId
    ) {
        return executionService.resumeExecution(tenantId, runId, checkpointId);
    }
}