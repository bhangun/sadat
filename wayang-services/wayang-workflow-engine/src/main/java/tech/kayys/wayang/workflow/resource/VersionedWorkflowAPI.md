package tech.kayys.wayang.workflow.resource;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;
import tech.kayys.wayang.workflow.repository.WorkflowRepository;

/**
 * API Versioning
 */
@ApplicationScoped
@Path("/api")
public class VersionedWorkflowAPI {

    @Inject
    WorkflowRepository workflowRegistry;

    @GET
    @Path("/v1/workflows/{id}")
    public Uni<WorkflowDefinition> getWorkflowV1(@PathParam("id") String id) {
        return workflowRegistry.findById(id, "default")
                .map(WorkflowDefinition::from);
    }

    @GET
    @Path("/v2/workflows/{id}")
    public Uni<WorkflowDefinition> getWorkflowV2(@PathParam("id") String id) {
        return workflowRegistry.findById(id, "default")
                .map(WorkflowDefinition::from);
    }

    /**
     * Version negotiation via header
     */
    @GET
    @Path("/workflows/{id}")
    public Uni<Response> getWorkflow(
            @PathParam("id") String id,
            @HeaderParam("API-Version") String apiVersion) {
        return switch (apiVersion) {
            case "1" -> getWorkflowV1(id).map(v -> Response.ok(v).build());
            case "2" -> getWorkflowV2(id).map(v -> Response.ok(v).build());
            default -> Uni.createFrom().item(
                    Response.status(400).entity("Unsupported API version").build());
        };
    }
}
