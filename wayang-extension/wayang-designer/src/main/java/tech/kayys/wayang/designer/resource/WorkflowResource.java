package tech.kayys.wayang.designer.resource;

import tech.kayys.wayang.common.domain.*;
import tech.kayys.wayang.designer.service.WorkflowService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/workflows")
@Tag(name = "Workflow Management")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkflowResource {
    
    @Inject
    WorkflowService workflowService;
    
    @POST
    @Operation(summary = "Create new workflow")
    public Uni<Response> createWorkflow(
        @HeaderParam("X-Tenant-ID") String tenantId,
        @HeaderParam("X-User-ID") String userId,
        @Valid WorkflowDefinition workflow
    ) {
        return workflowService.createWorkflow(tenantId, userId, workflow)
            .map(created -> Response
                .status(Response.Status.CREATED)
                .entity(created)
                .build()
            );
    }
    
    @GET
    @Path("/{workflowId}")
    public Uni<WorkflowDefinition> getWorkflow(
        @HeaderParam("X-Tenant-ID") String tenantId,
        @PathParam("workflowId") String workflowId
    ) {
        return workflowService.getWorkflow(tenantId, workflowId);
    }
    
    @PUT
    @Path("/{workflowId}")
    public Uni<WorkflowDefinition> updateWorkflow(
        @HeaderParam("X-Tenant-ID") String tenantId,
        @PathParam("workflowId") String workflowId,
        @Valid WorkflowDefinition workflow
    ) {
        return workflowService.updateWorkflow(tenantId, workflowId, workflow);
    }
    
    @POST
    @Path("/{workflowId}/validate")
    @Operation(summary = "Validate workflow before execution")
    public Uni<ValidationResult> validateWorkflow(
        @HeaderParam("X-Tenant-ID") String tenantId,
        @PathParam("workflowId") String workflowId
    ) {
        return workflowService.validateWorkflow(tenantId, workflowId);
    }
    
    @POST
    @Path("/{workflowId}/publish")
    public Uni<Response> publishWorkflow(
        @HeaderParam("X-Tenant-ID") String tenantId,
        @PathParam("workflowId") String workflowId,
        @QueryParam("version") String version
    ) {
        return workflowService.publishWorkflow(tenantId, workflowId, version)
            .map(published -> Response.ok(published).build());
    }
}