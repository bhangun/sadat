package tech.kayys.wayang.sdk;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import tech.kayys.wayang.sdk.dto.*;


import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

/**
 * Client for managing workflow definitions
 */
@Path("/api/v1/workflows")
@RegisterRestClient(configKey = "workflow-registry")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface WorkflowDefinitionClient {

    /**
     * Create a new workflow definition
     */
    @POST
    Uni<WorkflowDefinitionResponse> createWorkflow(WorkflowDefinitionRequest request);

    /**
     * Get workflow definition by ID
     */
    @GET
    @Path("/{workflowId}")
    Uni<WorkflowDefinitionResponse> getWorkflow(@PathParam("workflowId") String workflowId);

    /**
     * Update existing workflow definition
     */
    @PUT
    @Path("/{workflowId}")
    Uni<WorkflowDefinitionResponse> updateWorkflow(
        @PathParam("workflowId") String workflowId,
        WorkflowDefinitionRequest request
    );

    /**
     * Delete workflow definition
     */
    @DELETE
    @Path("/{workflowId}")
    Uni<Void> deleteWorkflow(@PathParam("workflowId") String workflowId);

    /**
     * List all workflows for tenant
     */
    @GET
    Uni<List<WorkflowDefinitionResponse>> listWorkflows(
        @QueryParam("tenantId") String tenantId,
        @QueryParam("tags") List<String> tags
    );

    /**
     * Validate workflow definition without saving
     */
    @POST
    @Path("/validate")
    Uni<ValidationResponse> validateWorkflow(WorkflowDefinitionRequest request);

    /**
     * Get workflow versions
     */
    @GET
    @Path("/{workflowId}/versions")
    Uni<List<WorkflowVersionResponse>> getWorkflowVersions(
        @PathParam("workflowId") String workflowId
    );

    /**
     * Publish workflow (make it executable)
     */
    @POST
    @Path("/{workflowId}/publish")
    Uni<WorkflowDefinitionResponse> publishWorkflow(
        @PathParam("workflowId") String workflowId,
        @QueryParam("version") String version
    );
}
