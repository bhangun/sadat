package tech.kayys.wayang.workflow.resource;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import tech.kayys.wayang.workflow.service.WorkflowRegistry;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;
import tech.kayys.wayang.workflow.api.dto.ErrorResponse;

import java.util.List;
import java.util.Map;

/**
 * WorkflowRegistryResource - REST API for workflow definition management
 */
@Path("/api/v1/workflows")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Workflow Registry", description = "Workflow definition management")
public class WorkflowRegistryResource {

    private static final Logger LOG = Logger.getLogger(WorkflowRegistryResource.class);

    @Inject
    WorkflowRegistry registry;

    @Context
    SecurityContext securityContext;

    @Context
    HttpHeaders httpHeaders;

    @POST
    @Operation(summary = "Register workflow", description = "Register a new workflow definition")
    public Uni<Response> registerWorkflow(WorkflowDefinition workflow) {
        LOG.infof("Registering workflow: %s version %s", workflow.getId(), workflow.getVersion());

        return registry.register(workflow)
                .map(w -> Response.status(Response.Status.CREATED)
                        .entity(w)
                        .build())
                .onFailure().recoverWithItem(th -> {
                    LOG.error("Failed to register workflow", th);
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(ErrorResponse.builder()
                                    .errorCode("REGISTRATION_FAILED")
                                    .message(th.getMessage())
                                    .build())
                            .build();
                });
    }

    @GET
    @Path("/{workflowId}")
    @Operation(summary = "Get workflow", description = "Get workflow definition by ID")
    public Uni<Response> getWorkflow(@PathParam("workflowId") String workflowId) {
        return registry.getWorkflow(workflowId)
                .map(w -> {
                    if (w == null) {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(ErrorResponse.builder()
                                        .errorCode("NOT_FOUND")
                                        .message("Workflow not found: " + workflowId)
                                        .build())
                                .build();
                    }
                    return Response.ok(w).build();
                });
    }

    @GET
    @Operation(summary = "List workflows", description = "List all registered workflows")
    public Uni<List<WorkflowDefinition>> listWorkflows() {
        return registry.getAllWorkflows();
    }

    @PUT
    @Path("/{workflowId}/versions/{version}/activate")
    @Operation(summary = "Activate workflow", description = "Activate a workflow version")
    public Uni<Response> activateWorkflow(
            @PathParam("workflowId") String workflowId,
            @PathParam("version") String version) {

        return registry.activate(workflowId, version)
                .map(v -> Response.ok().build())
                .onFailure().recoverWithItem(th -> Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("ACTIVATION_FAILED", th.getMessage()))
                        .build());
    }

    @PUT
    @Path("/{workflowId}/versions/{version}/deactivate")
    @Operation(summary = "Deactivate workflow", description = "Deactivate a workflow version")
    public Uni<Response> deactivateWorkflow(
            @PathParam("workflowId") String workflowId,
            @PathParam("version") String version) {

        return registry.deactivate(workflowId, version)
                .map(v -> Response.ok().build())
                .onFailure().recoverWithItem(th -> Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("DEACTIVATION_FAILED", th.getMessage()))
                        .build());
    }

    @DELETE
    @Path("/{workflowId}/versions/{version}")
    @Operation(summary = "Delete workflow", description = "Delete a workflow version")
    public Uni<Response> deleteWorkflow(
            @PathParam("workflowId") String workflowId,
            @PathParam("version") String version) {

        return registry.delete(workflowId, version)
                .map(v -> Response.noContent().build());
    }

    @POST
    @Path("/import")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(summary = "Import workflow", description = "Import workflow from definition content")
    public Uni<Response> importWorkflow(@QueryParam("id") String id, String content) {
        if (id == null || content == null) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponse.builder()
                            .errorCode("INVALID_REQUEST")
                            .message("ID and content are required")
                            .build())
                    .build());
        }

        return registry.importWorkflow(id, content)
                .map(w -> Response.status(Response.Status.CREATED).entity(w).build())
                .onFailure().recoverWithItem(th -> Response.status(Response.Status.BAD_REQUEST)
                        .entity(ErrorResponse.builder()
                                .errorCode("IMPORT_FAILED")
                                .message(th.getMessage())
                                .build())
                        .build());
    }

    @GET
    @Path("/{workflowId}/export")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Export workflow", description = "Export workflow definition as text")
    public Uni<Response> exportWorkflow(@PathParam("workflowId") String workflowId) {
        return registry.exportWorkflow(workflowId)
                .map(content -> {
                    if (content == null) {
                        return Response.status(Response.Status.NOT_FOUND).build();
                    }
                    return Response.ok(content).build();
                });
    }

    @GET
    @Path("/search")
    @Operation(summary = "Search workflows", description = "Search workflows by name pattern")
    public Uni<List<WorkflowDefinition>> searchWorkflows(
            @QueryParam("name") String namePattern) {
        String tenantId = getTenantId();
        return registry.search(tenantId, namePattern);
    }

    private String getTenantId() {
        // 1. Try X-Tenant-Id header
        String headerTenantId = httpHeaders.getHeaderString("X-Tenant-Id");
        if (headerTenantId != null && !headerTenantId.trim().isEmpty()) {
            return headerTenantId;
        }

        // 2. Try SecurityContext principal name
        if (securityContext != null && securityContext.getUserPrincipal() != null) {
            return securityContext.getUserPrincipal().getName();
        }

        // Default fallback
        return "default-tenant";
    }

    @GET
    @Path("/{workflowId}/validate")
    @Operation(summary = "Validate workflow", description = "Validate workflow structure and integrity")
    public Uni<Response> validateWorkflow(@PathParam("workflowId") String workflowId) {
        return registry.validate(workflowId)
                .map(isValid -> Response.ok(Map.of("valid", isValid)).build());
    }
}
