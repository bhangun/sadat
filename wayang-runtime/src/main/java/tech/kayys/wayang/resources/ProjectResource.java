package tech.kayys.wayang.resources;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.dto.ProjectCreatedEvent;
import tech.kayys.wayang.guardrails.service.GuardrailEngine;
import tech.kayys.wayang.project.domain.WayangProject;
import tech.kayys.wayang.project.dto.CreateProjectRequest;
import tech.kayys.wayang.project.dto.ProjectType;
import tech.kayys.wayang.project.service.ControlPlaneService;
import tech.kayys.wayang.security.service.AuthenticatedUser;
import tech.kayys.wayang.security.service.AuthorizationPolicyEngine;
import tech.kayys.wayang.security.service.IketSecurityService;
import tech.kayys.wayang.websocket.service.WebSocketEventBroadcaster;

@Path("/api/v1/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Projects", description = "Project management with guardrails")
@SecurityRequirement(name = "bearer-jwt")
public class ProjectResource {

    @Inject
    ControlPlaneService controlPlaneService;

    @Inject
    IketSecurityService iketSecurity;

    @Inject
    AuthorizationPolicyEngine authzEngine;

    @Inject
    GuardrailEngine guardrailEngine;

    @Inject
    WebSocketEventBroadcaster wsEventBroadcaster;

    @POST
    @Operation(summary = "Create project with guardrails")
    @RolesAllowed({ "admin", "project_manager" })
    public Uni<RestResponse<WayangProject>> createProject(
            @Valid CreateProjectRequest request) {

        AuthenticatedUser user = iketSecurity.getCurrentUser();

        // Check authorization
        if (!authzEngine.authorize(user, "project", "create")) {
            return Uni.createFrom().item(
                    RestResponse.status(RestResponse.Status.FORBIDDEN));
        }

        // Create project
        return controlPlaneService.createProject(
                new CreateProjectRequest(
                        user.tenantId(),
                        request.projectName(),
                        request.description(),
                        request.projectType(),
                        user.userId(),
                        request.metadata()))
                .flatMap(project -> {
                    // Broadcast event
                    return wsEventBroadcaster.broadcastToTenant(
                            user.tenantId(),
                            new ProjectCreatedEvent(
                                    project.projectId,
                                    project.projectName,
                                    user.userId(),
                                    Instant.now()))
                            .map(v -> RestResponse.status(
                                    RestResponse.Status.CREATED, project));
                });
    }

    @GET
    @Path("/{projectId}")
    @Operation(summary = "Get project")
    public Uni<RestResponse<WayangProject>> getProject(
            @PathParam("projectId") UUID projectId) {

        AuthenticatedUser user = iketSecurity.getCurrentUser();

        return controlPlaneService.getProject(projectId, user.tenantId())
                .map(project -> {
                    if (project == null) {
                        return RestResponse.notFound();
                    }

                    // Check authorization with resource attributes
                    if (!authzEngine.authorizeWithAttributes(
                            user,
                            "project",
                            "read",
                            Map.of("project_id", projectId.toString()))) {
                        return RestResponse.status(RestResponse.Status.FORBIDDEN);
                    }

                    return RestResponse.ok(project);
                });
    }

    @GET
    @Operation(summary = "List projects")
    public Uni<List<WayangProject>> listProjects(
            @QueryParam("type") ProjectType projectType) {

        AuthenticatedUser user = iketSecurity.getCurrentUser();
        return controlPlaneService.listProjects(user.tenantId(), projectType);
    }

}