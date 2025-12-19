package tech.kayys.wayang.agent.resource;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestPath;
import tech.kayys.wayang.agent.dto.*;
import tech.kayys.wayang.agent.service.AgentBuilderService;

import java.util.List;

/**
 * Agent Builder REST API - UI-focused endpoints for low-code agent building
 */
@Path("/api/v1/agent-builder")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AgentBuilderResource {

    @Inject
    AgentBuilderService agentBuilderService;

    /**
     * Create new agent via builder UI
     */
    @POST
    @Path("/agents")
    public Uni<Response> createAgent(AgentBuilderRequest request) {
        Log.info("Received request to create agent via builder UI");
        return agentBuilderService.createAgent(request)
                .map(agentResponse -> Response.status(Response.Status.CREATED)
                        .entity(agentResponse)
                        .build())
                .onFailure().recoverWithItem(throwable -> {
                    Log.error("Error creating agent via builder", throwable);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("Error creating agent: " + throwable.getMessage())
                            .build();
                });
    }

    /**
     * Update existing agent via builder UI
     */
    @PUT
    @Path("/agents/{agentId}")
    public Uni<Response> updateAgent(@RestPath String agentId, AgentBuilderUpdateRequest request) {
        Log.infof("Received request to update agent via builder: %s", agentId);
        return agentBuilderService.updateAgent(agentId, request)
                .map(agentResponse -> Response.ok(agentResponse).build())
                .onFailure().recoverWithItem(throwable -> {
                    Log.error("Error updating agent via builder", throwable);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("Error updating agent: " + throwable.getMessage())
                            .build();
                });
    }

    /**
     * Get agent details for builder UI
     */
    @GET
    @Path("/agents/{agentId}")
    public Uni<Response> getAgentForBuilder(@RestPath String agentId) {
        Log.infof("Received request to get agent for builder UI: %s", agentId);
        return agentBuilderService.getAgentForBuilder(agentId)
                .map(agentDetail -> Response.ok(agentDetail).build())
                .onFailure().recoverWithItem(throwable -> {
                    Log.error("Error getting agent for builder", throwable);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("Error getting agent: " + throwable.getMessage())
                            .build();
                });
    }

    /**
     * List agents for builder UI
     */
    @GET
    @Path("/agents")
    public Uni<Response> listAgentsForBuilder(
            @QueryParam("tenantId") String tenantId,
            @QueryParam("agentType") AgentType agentType,
            @QueryParam("status") String status,
            @QueryParam("search") String search,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        
        return agentBuilderService.listAgentsForBuilder(tenantId, agentType, status, search, page, size)
                .map(listResponse -> Response.ok(listResponse).build())
                .onFailure().recoverWithItem(throwable -> {
                    Log.error("Error listing agents for builder", throwable);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("Error listing agents: " + throwable.getMessage())
                            .build();
                });
    }

    /**
     * Execute agent from builder UI
     */
    @POST
    @Path("/agents/{agentId}/execute")
    public Uni<Response> executeAgent(@RestPath String agentId, AgentExecutionRequest request) {
        Log.infof("Received request to execute agent via builder: %s", agentId);
        return agentBuilderService.executeAgent(agentId, request)
                .map(result -> Response.ok(result).build())
                .onFailure().recoverWithItem(throwable -> {
                    Log.error("Error executing agent via builder", throwable);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("Error executing agent: " + throwable.getMessage())
                            .build();
                });
    }

    /**
     * Get available tools for builder UI
     */
    @GET
    @Path("/tools")
    public Uni<Response> getAvailableTools() {
        Log.info("Received request for available tools");
        return agentBuilderService.getAvailableTools()
                .map(tools -> Response.ok(tools).build())
                .onFailure().recoverWithItem(throwable -> {
                    Log.error("Error fetching available tools", throwable);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("Error fetching tools: " + throwable.getMessage())
                            .build();
                });
    }

    /**
     * Get available LLM providers for builder UI
     */
    @GET
    @Path("/providers")
    public Uni<Response> getAvailableProviders() {
        Log.info("Received request for available LLM providers");
        return agentBuilderService.getAvailableProviders()
                .map(providers -> Response.ok(providers).build())
                .onFailure().recoverWithItem(throwable -> {
                    Log.error("Error fetching available providers", throwable);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("Error fetching providers: " + throwable.getMessage())
                            .build();
                });
    }

    /**
     * Get agent execution history
     */
    @GET
    @Path("/agents/{agentId}/executions")
    public Uni<Response> getAgentExecutions(@RestPath String agentId) {
        // This would be implemented in the service to fetch execution history
        // For now, returning a mock response
        return Uni.createFrom().item(Response.ok(List.of()).build());
    }
}