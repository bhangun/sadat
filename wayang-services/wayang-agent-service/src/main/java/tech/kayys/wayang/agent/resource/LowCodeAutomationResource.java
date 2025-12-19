package tech.kayys.wayang.agent.resource;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestPath;
import tech.kayys.wayang.agent.dto.*;
import tech.kayys.wayang.agent.service.BusinessRuleService;
import tech.kayys.wayang.agent.service.LowCodeAutomationService;

import java.util.Map;

@Path("/api/v1/lowcode")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LowCodeAutomationResource {

    @Inject
    LowCodeAutomationService automationService;
    
    @Inject
    BusinessRuleService ruleService;

    @POST
    @Path("/agents")
    public Uni<Response> createAgent(AgentWorkflowRequest request) {
        Log.info("Received request to create agent");
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
    @Path("/workflows")
    public Uni<Response> createWorkflow(AgentWorkflowRequest request) {
        Log.info("Received request to create workflow");
        return automationService.createWorkflow(request)
                .map(workflow -> Response.status(Response.Status.CREATED)
                        .entity(workflow)
                        .build())
                .onFailure().recoverWithItem(throwable -> {
                    Log.error("Error creating workflow", throwable);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("Error creating workflow: " + throwable.getMessage())
                            .build();
                });
    }

    @POST
    @Path("/agents/{agentId}/execute")
    public Uni<Response> executeAgent(@RestPath String agentId, AgentExecutionRequest request) {
        Log.infof("Received request to execute agent: %s", agentId);
        return automationService.executeAgent(agentId, request)
                .map(result -> Response.ok(result).build())
                .onFailure().recoverWithItem(throwable -> {
                    Log.error("Error executing agent", throwable);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("Error executing agent: " + throwable.getMessage())
                            .build();
                });
    }

    @POST
    @Path("/integrations")
    public Uni<Response> createIntegration(IntegrationDefinition integrationDef) {
        Log.info("Received request to create integration");
        return automationService.createIntegration(integrationDef)
                .map(integration -> Response.status(Response.Status.CREATED)
                        .entity(integration)
                        .build())
                .onFailure().recoverWithItem(throwable -> {
                    Log.error("Error creating integration", throwable);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("Error creating integration: " + throwable.getMessage())
                            .build();
                });
    }

    @POST
    @Path("/business-rules")
    public Uni<Response> createBusinessRule(String ruleDefinition) {
        Log.info("Received request to create business rule");
        return ruleService.createBusinessRule(ruleDefinition)
                .map(ruleId -> Response.status(Response.Status.CREATED)
                        .entity(Map.of("id", ruleId, "status", "created"))
                        .build())
                .onFailure().recoverWithItem(throwable -> {
                    Log.error("Error creating business rule", throwable);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("Error creating business rule: " + throwable.getMessage())
                            .build();
                });
    }

    @POST
    @Path("/business-rules/{ruleId}/execute")
    public Uni<Response> executeBusinessRule(@RestPath String ruleId, Object context) {
        Log.infof("Received request to execute business rule: %s", ruleId);
        return ruleService.executeBusinessRule(ruleId, context)
                .map(result -> Response.ok(Map.of("result", result)).build())
                .onFailure().recoverWithItem(throwable -> {
                    Log.error("Error executing business rule", throwable);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("Error executing business rule: " + throwable.getMessage())
                            .build();
                });
    }
}