package tech.kayys.wayang.agent.resource;

import java.util.Map;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.agent.model.Workflow;
import tech.kayys.wayang.agent.repository.AgentDefinitionRepository;
import tech.kayys.wayang.workflow.model.ExecutionContext;
import tech.kayys.wayang.workflow.service.ExecutionContextManager;
import tech.kayys.wayang.workflow.service.WorkflowRuntimeEngine;

/**
 * Webhook API for triggers
 */
@Path("/api/v1/webhooks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WebhookApi {

    private static final Logger LOG = Logger.getLogger(WebhookApi.class);

    @Inject
    AgentDefinitionRepository repository;

    @Inject
    WorkflowRuntimeEngine workflowEngine;

    @Inject
    ExecutionContextManager contextManager;

    /**
     * Webhook trigger endpoint
     */
    @POST
    @Path("/{agentId}/{workflowName}")
    public Uni<Response> triggerWebhook(
            @PathParam("agentId") String agentId,
            @PathParam("workflowName") String workflowName,
            @HeaderParam("X-Webhook-Secret") String secret,
            Map<String, Object> payload) {

        LOG.infof("Webhook triggered: agent=%s, workflow=%s", agentId, workflowName);

        // TODO: Validate webhook secret

        return repository.findAgentById(agentId)
                .chain(agent -> {
                    if (agent == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND).build());
                    }

                    Workflow workflow = agent.getWorkflows().stream()
                            .filter(w -> w.getName().equals(workflowName))
                            .findFirst()
                            .orElse(null);

                    if (workflow == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND).build());
                    }

                    ExecutionContext context = contextManager.createContext();

                    return workflowEngine.executeWorkflow(workflow, payload, context)
                            .map(result -> Response.ok(Map.of(
                                    "executionId", context.getExecutionId(),
                                    "success", result.isSuccess())).build());
                });
    }
}