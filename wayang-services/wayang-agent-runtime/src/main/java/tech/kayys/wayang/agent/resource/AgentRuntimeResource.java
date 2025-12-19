package tech.kayys.wayang.agent.resource;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.agent.model.AgentDefinition;
import tech.kayys.wayang.agent.model.Workflow;
import tech.kayys.wayang.agent.repository.AgentRepository;
import tech.kayys.wayang.workflow.model.ExecutionContext;
import tech.kayys.wayang.workflow.service.ExecutionContextManager;
import tech.kayys.wayang.workflow.service.WorkflowRuntimeEngine;

import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

/**
 * REST API for AI Agent Runtime
 */
@Path("/api/agents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AgentRuntimeResource {

    private static final Logger LOG = Logger.getLogger(AgentRuntimeResource.class);

    @Inject
    AgentRepository agentRepository;

    @Inject
    WorkflowRuntimeEngine runtimeEngine;

    @Inject
    ExecutionContextManager contextManager;

    /**
     * Create or update agent definition
     */
    @POST
    public Uni<Response> createAgent(AgentDefinition agent) {
        LOG.infof("Creating agent: %s", agent.getName());

        return agentRepository.save(agent)
                .map(saved -> Response.status(Response.Status.CREATED)
                        .entity(saved)
                        .build());
    }

    /**
     * Get agent by ID
     */
    @GET
    @Path("/{agentId}")
    public Uni<Response> getAgent(@PathParam("agentId") String agentId) {
        return agentRepository.findById(agentId)
                .map(agent -> agent != null ? Response.ok(agent).build()
                        : Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * List all agents
     */
    @GET
    public Uni<Response> listAgents(
            @QueryParam("status") AgentDefinition.AgentStatus status,
            @QueryParam("type") AgentDefinition.AgentType type) {

        return agentRepository.findAll(status, type)
                .map(agents -> Response.ok(agents).build());
    }

    /**
     * Delete agent
     */
    @DELETE
    @Path("/{agentId}")
    public Uni<Response> deleteAgent(@PathParam("agentId") String agentId) {
        return agentRepository.delete(agentId)
                .map(deleted -> deleted ? Response.noContent().build()
                        : Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * Execute agent workflow
     */
    @POST
    @Path("/{agentId}/execute")
    public Uni<Response> executeAgent(
            @PathParam("agentId") String agentId,
            ExecutionRequest request) {

        LOG.infof("Executing agent: %s", agentId);

        return agentRepository.findById(agentId)
                .chain(agent -> {
                    if (agent == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "Agent not found"))
                                        .build());
                    }

                    // Find workflow by name or use first workflow
                    Workflow workflow = findWorkflow(agent, request.workflowName);
                    if (workflow == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.BAD_REQUEST)
                                        .entity(Map.of("error", "Workflow not found"))
                                        .build());
                    }

                    // Create execution context
                    ExecutionContext context = contextManager.createContext();

                    // Execute workflow
                    return runtimeEngine.executeWorkflow(workflow, request.input, context)
                            .map(result -> Response.ok(new ExecutionResponse(
                                    context.getExecutionId(),
                                    result.isSuccess(),
                                    result.getOutput(),
                                    result.getTrace(),
                                    result.getError())).build());
                });
    }

    /**
     * Execute specific workflow
     */
    @POST
    @Path("/{agentId}/workflows/{workflowId}/execute")
    public Uni<Response> executeWorkflow(
            @PathParam("agentId") String agentId,
            @PathParam("workflowId") String workflowId,
            Map<String, Object> input) {

        return agentRepository.findById(agentId)
                .chain(agent -> {
                    if (agent == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND).build());
                    }

                    Workflow workflow = agent.getWorkflows().stream()
                            .filter(w -> w.getId().equals(workflowId))
                            .findFirst()
                            .orElse(null);

                    if (workflow == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND).build());
                    }

                    ExecutionContext context = contextManager.createContext();

                    return runtimeEngine.executeWorkflow(workflow, input, context)
                            .map(result -> Response.ok(new ExecutionResponse(
                                    context.getExecutionId(),
                                    result.isSuccess(),
                                    result.getOutput(),
                                    result.getTrace(),
                                    result.getError())).build());
                });
    }

    /**
     * Get execution status
     */
    @GET
    @Path("/executions/{executionId}")
    public Response getExecutionStatus(@PathParam("executionId") String executionId) {
        ExecutionContext context = contextManager.getContext(executionId);

        if (context == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(Map.of(
                "executionId", executionId,
                "duration", context.getExecutionDuration(),
                "trace", context.getExecutionTrace())).build();
    }

    /**
     * Stream agent execution (Server-Sent Events)
     */
    @GET
    @Path("/{agentId}/execute/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<String> streamExecution(
            @PathParam("agentId") String agentId,
            @QueryParam("workflowName") String workflowName) {

        // Placeholder for SSE streaming implementation
        return Multi.createFrom().empty();
    }

    /**
     * Find workflow by name
     */
    private Workflow findWorkflow(AgentDefinition agent, String workflowName) {
        if (agent.getWorkflows() == null || agent.getWorkflows().isEmpty()) {
            return null;
        }

        if (workflowName != null) {
            return agent.getWorkflows().stream()
                    .filter(w -> w.getName().equals(workflowName))
                    .findFirst()
                    .orElse(null);
        }

        return agent.getWorkflows().get(0);
    }

    /**
     * Execution request
     */
    public static class ExecutionRequest {
        public String workflowName;
        public Map<String, Object> input;
        public Map<String, Object> config;

        public ExecutionRequest() {
        }

        public ExecutionRequest(String workflowName, Map<String, Object> input) {
            this.workflowName = workflowName;
            this.input = input;
        }
    }

    /**
     * Execution response
     */
    public static class ExecutionResponse {
        public String executionId;
        public boolean success;
        public Map<String, Object> output;
        public List<WorkflowRuntimeEngine.ExecutionTrace> trace;
        public String error;

        public ExecutionResponse() {
        }

        public ExecutionResponse(String executionId, boolean success,
                Map<String, Object> output,
                List<WorkflowRuntimeEngine.ExecutionTrace> trace,
                String error) {
            this.executionId = executionId;
            this.success = success;
            this.output = output;
            this.trace = trace;
            this.error = error;
        }
    }
}
