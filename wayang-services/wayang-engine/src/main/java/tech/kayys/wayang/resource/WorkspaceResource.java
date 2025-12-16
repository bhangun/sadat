package tech.kayys.wayang.resource;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.ResponseStatus;
import tech.kayys.wayang.domain.Workspace;
import tech.kayys.wayang.service.WorkspaceService;
import tech.kayys.wayang.tenant.TenantContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * WorkspaceResource - REST API for workspace management
 */
@Path("/api/v1/workspaces")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Workspaces", description = "Workspace management operations")
@RolesAllowed({ "admin", "designer" })
public class WorkspaceResource {

    private static final Logger LOG = Logger.getLogger(WorkspaceResource.class);

    @Inject
    WorkspaceService workspaceService;

    @Inject
    TenantContext tenantContext;

    @GET
    @Operation(summary = "List workspaces", description = "List all workspaces for current tenant")
    @APIResponse(responseCode = "200", description = "Success")
    public Uni<Response> listWorkspaces() {
        LOG.infof("Listing workspaces for tenant: %s", tenantContext.getTenantId());
        return workspaceService.listWorkspaces()
                .map(workspaces -> Response.ok(WorkspaceListResponse.fromWorkspaces(workspaces)).build());
    }

    @POST
    @Operation(summary = "Create workspace", description = "Create a new workspace")
    @APIResponse(responseCode = "201", description = "Created")
    @ResponseStatus(201)
    public Uni<Response> createWorkspace(@Valid CreateWorkspaceDTO request) {
        LOG.infof("Creating workspace '%s' for tenant: %s", request.name, tenantContext.getTenantId());
        WorkspaceService.CreateWorkspaceRequest serviceRequest = new WorkspaceService.CreateWorkspaceRequest();
        serviceRequest.name = request.name;
        serviceRequest.description = request.description;
        serviceRequest.metadata = request.metadata;

        return workspaceService.createWorkspace(serviceRequest)
                .map(workspace -> Response.created(
                        UriBuilder.fromResource(WorkspaceResource.class)
                                .path(workspace.id.toString())
                                .build())
                        .entity(new WorkspaceResponse(workspace))
                        .build());
    }

    @GET
    @Path("/{workspaceId}")
    @Operation(summary = "Get workspace", description = "Get workspace by ID")
    @APIResponse(responseCode = "200", description = "Success")
    @APIResponse(responseCode = "404", description = "Workspace not found")
    public Uni<Response> getWorkspace(@PathParam("workspaceId") UUID workspaceId) {
        LOG.debugf("Getting workspace %s for tenant: %s", workspaceId, tenantContext.getTenantId());
        return workspaceService.getWorkspace(workspaceId)
                .map(workspace -> Response.ok(new WorkspaceResponse(workspace)).build());
    }

    @PUT
    @Path("/{workspaceId}")
    @Operation(summary = "Update workspace", description = "Update workspace details")
    @APIResponse(responseCode = "200", description = "Success")
    public Uni<Response> updateWorkspace(
            @PathParam("workspaceId") UUID workspaceId,
            @Valid UpdateWorkspaceDTO request) {

        LOG.infof("Updating workspace %s for tenant: %s", workspaceId, tenantContext.getTenantId());
        WorkspaceService.UpdateWorkspaceRequest serviceRequest = new WorkspaceService.UpdateWorkspaceRequest();
        serviceRequest.name = request.name;
        serviceRequest.description = request.description;
        serviceRequest.metadata = request.metadata;

        return workspaceService.updateWorkspace(workspaceId, serviceRequest)
                .map(workspace -> Response.ok(new WorkspaceResponse(workspace)).build());
    }

    @DELETE
    @Path("/{workspaceId}")
    @Operation(summary = "Delete workspace", description = "Delete workspace (soft delete)")
    @APIResponse(responseCode = "204", description = "Deleted")
    public Uni<Response> deleteWorkspace(@PathParam("workspaceId") UUID workspaceId) {
        LOG.infof("Deleting workspace %s for tenant: %s", workspaceId, tenantContext.getTenantId());
        return workspaceService.deleteWorkspace(workspaceId)
                .replaceWith(Response.noContent().build());
    }

    // DTOs

    public static class CreateWorkspaceDTO {
        @NotBlank(message = "Workspace name is required")
        public String name;
        public String description;
        public Map<String, Object> metadata;
    }

    public static class UpdateWorkspaceDTO {
        public String name;
        public String description;
        public Map<String, Object> metadata;
    }

    public record WorkspaceResponse(
            UUID id,
            String name,
            String description,
            String tenantId,
            String ownerId,
            Workspace.WorkspaceStatus status,
            Map<String, Object> metadata,
            java.time.Instant createdAt,
            java.time.Instant updatedAt) {

        public WorkspaceResponse(Workspace workspace) {
            this(
                    workspace.id,
                    workspace.name,
                    workspace.description,
                    workspace.tenantId,
                    workspace.ownerId,
                    workspace.status,
                    workspace.metadata,
                    workspace.createdAt,
                    workspace.updatedAt);
        }
    }

    public record WorkspaceListResponse(List<WorkspaceResponse> workspaces) {
        public static WorkspaceListResponse fromWorkspaces(List<Workspace> workspaces) {
            return new WorkspaceListResponse(workspaces.stream().map(WorkspaceResponse::new).toList());
        }
    }
}
