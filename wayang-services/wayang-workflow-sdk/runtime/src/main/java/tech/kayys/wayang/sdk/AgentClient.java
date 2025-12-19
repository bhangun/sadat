package tech.kayys.wayang.sdk;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;




import tech.kayys.wayang.sdk.dto.agent.*;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;
import java.util.Map;

/**
 * Client for agent-specific operations
 */
@Path("/api/v1/agents")
@RegisterRestClient(configKey = "agent-runtime")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface AgentClient {

    /**
     * Execute agent with direct invocation
     */
    @POST
    @Path("/{agentId}/execute")
    Uni<AgentExecutionResponse> executeAgent(
        @PathParam("agentId") String agentId,
        AgentExecutionRequest request
    );

    /**
     * Get agent capabilities and metadata
     */
    @GET
    @Path("/{agentId}")
    Uni<AgentInfoResponse> getAgentInfo(@PathParam("agentId") String agentId);

    /**
     * List available agents
     */
    @GET
    Uni<List<AgentInfoResponse>> listAgents(
        @QueryParam("role") String role,
        @QueryParam("capability") List<String> capabilities
    );

    /**
     * Register a new agent
     */
    @POST
    Uni<AgentInfoResponse> registerAgent(AgentRegistrationRequest request);

    /**
     * Update agent configuration
     */
    @PUT
    @Path("/{agentId}/config")
    Uni<Void> updateAgentConfig(
        @PathParam("agentId") String agentId,
        Map<String, Object> config
    );

    /**
     * Get agent execution history
     */
    @GET
    @Path("/{agentId}/history")
    Uni<List<AgentExecutionRecord>> getAgentHistory(
        @PathParam("agentId") String agentId,
        @QueryParam("limit") @DefaultValue("50") int limit
    );
}
