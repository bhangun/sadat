package tech.kayys.wayang.agent.resource;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestPath;
import tech.kayys.wayang.agent.dto.AgentWorkflowRequest;
import tech.kayys.wayang.agent.dto.AgentExecutionRequest;
import tech.kayys.wayang.agent.service.LowCodeAutomationService;

import java.util.Map;

@Path("/api/v1/agents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AgentServiceResource {

        @Inject
        LowCodeAutomationService automationService;

        @POST
        public Uni<Response> createAgent(AgentWorkflowRequest request) {
                Log.info("Creating agent via agent service");
                return automationService.createAgent(request)
                                .map(agentDef -> Response.status(Response.Status.CREATED)
                                                .entity(agentDef)
                                                .build())
                                .onFailure().recoverWithItem(throwable -> {
                                        Log.error("Error creating agent", throwable);
                                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                                        .entity("Error creating agent: " + throwable.getMessage())
                                                        .build();
                                });
        }

        @POST
        @Path("/{agentId}/execute")
        public Uni<Response> executeAgent(@RestPath String agentId, AgentExecutionRequest request) {
                Log.infof("Executing agent: %s", agentId);
                return automationService.executeAgent(agentId, request)
                                .map(response -> Response.ok(response)
                                                .build())
                                .onFailure().recoverWithItem(throwable -> {
                                        Log.error("Error executing agent", throwable);
                                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                                        .entity("Error executing agent: " + throwable.getMessage())
                                                        .build();
                                });
        }

        @GET
        @Path("/{agentId}")
        public Uni<Response> getAgent(@RestPath String agentId) {
                Log.infof("Getting agent: %s", agentId);
                // In a real implementation, we would fetch the agent details
                return Uni.createFrom().item(
                                Response.ok(Map.of("id", agentId, "name", "Sample Agent", "status", "active"))
                                                .build());
        }
}