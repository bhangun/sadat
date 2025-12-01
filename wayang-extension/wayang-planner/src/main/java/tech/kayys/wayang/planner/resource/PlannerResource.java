package tech.kayys.wayang.planner.resource;

import tech.kayys.wayang.common.domain.*;
import tech.kayys.wayang.planner.service.PlanningService;
import tech.kayys.wayang.planner.domain.*;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/planner")
@Tag(name = "Planning Engine")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PlannerResource {
    
    @Inject
    PlanningService planningService;
    
    @POST
    @Path("/plan")
    @Operation(summary = "Create execution plan from goal")
    public Uni<ExecutionPlan> createPlan(
        @HeaderParam("X-Tenant-ID") String tenantId,
        @HeaderParam("X-User-ID") String userId,
        @Valid PlanRequest request
    ) {
        return planningService.createPlan(tenantId, userId, request);
    }
    
    @POST
    @Path("/{planId}/validate")
    @Operation(summary = "Validate plan for execution")
    public Uni<ValidationResult> validatePlan(
        @HeaderParam("X-Tenant-ID") String tenantId,
        @PathParam("planId") String planId
    ) {
        return planningService.validatePlan(tenantId, planId);
    }
    
    @POST
    @Path("/{planId}/revise")
    @Operation(summary = "Revise plan based on feedback")
    public Uni<ExecutionPlan> revisePlan(
        @HeaderParam("X-Tenant-ID") String tenantId,
        @PathParam("planId") String planId,
        @Valid RevisionRequest revision
    ) {
        return planningService.revisePlan(tenantId, planId, revision);
    }
    
    @GET
    @Path("/{planId}/estimate")
    @Operation(summary = "Estimate cost and resources")
    public Uni<PlanEstimate> estimatePlan(
        @HeaderParam("X-Tenant-ID") String tenantId,
        @PathParam("planId") String planId
    ) {
        return planningService.estimatePlan(tenantId, planId);
    }
}
