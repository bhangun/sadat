package tech.kayys.silat.api.subworkflow;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;
import tech.kayys.silat.api.security.TenantContext;
import tech.kayys.silat.core.domain.*;
import tech.kayys.silat.executor.subworkflow.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * SUB-WORKFLOW MANAGEMENT API
 * ============================================================================
 *
 * REST API for managing sub-workflow relationships and monitoring.
 *
 * Features:
 * - Query parent-child relationships
 * - Monitor sub-workflow execution
 * - Manage cross-tenant permissions
 * - View sub-workflow hierarchy
 * - Cancel cascading workflows
 *
 * @author Silat Team
 */

@Path("/api/v1/sub-workflows")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Sub-Workflows", description = "Sub-workflow relationship and monitoring")
public class SubWorkflowResource {

    @Inject
    SubWorkflowRelationshipService relationshipService;

    @Inject
    SubWorkflowHierarchyService hierarchyService;

    @Inject
    TenantContext tenantContext;

    // ==================== RELATIONSHIP QUERIES ====================

    @GET
    @Path("/parent/{parentRunId}/children")
    @Operation(summary = "Get child workflows for a parent")
    public Uni<List<ChildWorkflowInfo>> getChildren(
            @PathParam("parentRunId") String parentRunId) {

        String tenantId = tenantContext.getCurrentTenantId().value();

        return relationshipService.getChildWorkflows(
            WorkflowRunId.of(parentRunId),
            TenantId.of(tenantId)
        );
    }

    @GET
    @Path("/child/{childRunId}/parent")
    @Operation(summary = "Get parent workflow for a child")
    public Uni<RestResponse<ParentWorkflowInfo>> getParent(
            @PathParam("childRunId") String childRunId) {

        String tenantId = tenantContext.getCurrentTenantId().value();

        return relationshipService.getParentWorkflow(
                WorkflowRunId.of(childRunId),
                TenantId.of(tenantId))
            .map(parent -> parent != null ?
                RestResponse.ok(parent) :
                RestResponse.notFound());
    }

    @GET
    @Path("/{runId}/hierarchy")
    @Operation(summary = "Get complete workflow hierarchy")
    public Uni<WorkflowHierarchy> getHierarchy(
            @PathParam("runId") String runId,
            @QueryParam("maxDepth") @DefaultValue("10") int maxDepth) {

        String tenantId = tenantContext.getCurrentTenantId().value();

        return hierarchyService.buildHierarchy(
            WorkflowRunId.of(runId),
            TenantId.of(tenantId),
            maxDepth
        );
    }

    @GET
    @Path("/{runId}/ancestors")
    @Operation(summary = "Get all ancestor workflows")
    public Uni<List<WorkflowAncestor>> getAncestors(
            @PathParam("runId") String runId) {

        String tenantId = tenantContext.getCurrentTenantId().value();

        return hierarchyService.getAncestors(
            WorkflowRunId.of(runId),
            TenantId.of(tenantId)
        );
    }

    @GET
    @Path("/{runId}/descendants")
    @Operation(summary = "Get all descendant workflows")
    public Uni<List<WorkflowDescendant>> getDescendants(
            @PathParam("runId") String runId) {

        String tenantId = tenantContext.getCurrentTenantId().value();

        return hierarchyService.getDescendants(
            WorkflowRunId.of(runId),
            TenantId.of(tenantId)
        );
    }

    // ==================== MONITORING ====================

    @GET
    @Path("/{runId}/status/aggregate")
    @Operation(summary = "Get aggregated status including all sub-workflows")
    public Uni<AggregatedWorkflowStatus> getAggregatedStatus(
            @PathParam("runId") String runId) {

        String tenantId = tenantContext.getCurrentTenantId().value();

        return hierarchyService.getAggregatedStatus(
            WorkflowRunId.of(runId),
            TenantId.of(tenantId)
        );
    }

    @GET
    @Path("/{runId}/metrics/aggregate")
    @Operation(summary = "Get aggregated metrics including all sub-workflows")
    public Uni<AggregatedWorkflowMetrics> getAggregatedMetrics(
            @PathParam("runId") String runId) {

        String tenantId = tenantContext.getCurrentTenantId().value();

        return hierarchyService.getAggregatedMetrics(
            WorkflowRunId.of(runId),
            TenantId.of(tenantId)
        );
    }

    // ==================== CANCELLATION ====================

    @POST
    @Path("/{runId}/cancel/cascade")
    @Operation(summary = "Cancel workflow and all descendants")
    public Uni<CascadeCancellationResult> cancelCascade(
            @PathParam("runId") String runId,
            @Valid CancelCascadeRequest request) {

        String tenantId = tenantContext.getCurrentTenantId().value();

        return relationshipService.cancelCascade(
            WorkflowRunId.of(runId),
            TenantId.of(tenantId),
            request.reason()
        );
    }

    // ==================== CROSS-TENANT PERMISSIONS ====================

    @GET
    @Path("/permissions/cross-tenant")
    @Operation(summary = "Get cross-tenant sub-workflow permissions")
    public Uni<List<CrossTenantPermission>> getCrossTenantPermissions() {

        String tenantId = tenantContext.getCurrentTenantId().value();

        return relationshipService.getCrossTenantPermissions(
            TenantId.of(tenantId)
        );
    }

    @POST
    @Path("/permissions/cross-tenant")
    @Operation(summary = "Grant cross-tenant sub-workflow permission")
    public Uni<RestResponse<CrossTenantPermission>> grantCrossTenantPermission(
            @Valid GrantCrossTenantPermissionRequest request) {

        String sourceTenantId = tenantContext.getCurrentTenantId().value();

        return relationshipService.grantCrossTenantPermission(
                TenantId.of(sourceTenantId),
                TenantId.of(request.targetTenantId()),
                request.permissions())
            .map(permission -> RestResponse.status(
                RestResponse.Status.CREATED, permission));
    }

    @DELETE
    @Path("/permissions/cross-tenant/{targetTenantId}")
    @Operation(summary = "Revoke cross-tenant sub-workflow permission")
    public Uni<RestResponse<Void>> revokeCrossTenantPermission(
            @PathParam("targetTenantId") String targetTenantId) {

        String sourceTenantId = tenantContext.getCurrentTenantId().value();

        return relationshipService.revokeCrossTenantPermission(
                TenantId.of(sourceTenantId),
                TenantId.of(targetTenantId))
            .map(v -> RestResponse.ok());
    }
}