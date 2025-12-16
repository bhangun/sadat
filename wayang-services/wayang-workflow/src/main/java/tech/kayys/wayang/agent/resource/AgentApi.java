package tech.kayys.wayang.agent.resource;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import tech.kayys.wayang.agent.model.AgentDefinition;
import tech.kayys.wayang.agent.model.OrchestrationPattern;
import tech.kayys.wayang.agent.model.Workflow;
import tech.kayys.wayang.agent.repository.AgentDefinitionRepository;
import tech.kayys.wayang.orchestration.engine.OrchestrationEngine;
import tech.kayys.wayang.workflow.model.ExecutionContext;
import tech.kayys.wayang.workflow.repository.WorkflowExecutionRepository;
import tech.kayys.wayang.workflow.service.ExecutionContextManager;
import tech.kayys.wayang.workflow.service.WorkflowRuntimeEngine;

import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.*;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.*;

/**
 * Production-Ready Agent API with validation, security, and monitoring
 */
@Path("/api/v1/agents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Counted(name = "agent_api_requests", description = "Total agent API requests")
@Timed(name = "agent_api_timer", description = "Agent API response time", unit = MetricUnits.MILLISECONDS)
public class AgentApi {

    private static final Logger LOG = Logger.getLogger(AgentApi.class);

    @Inject
    AgentDefinitionRepository repository;

    @Inject
    WorkflowRuntimeEngine workflowEngine;

    @Inject
    OrchestrationEngine orchestrationEngine;

    @Inject
    ExecutionContextManager contextManager;

    @Inject
    WorkflowExecutionRepository executionRepository;

    @Context
    SecurityContext securityContext;

    /**
     * Create new agent
     */
    @POST
    @ApiKeyAuth
    @Counted(name = "agents_created", description = "Total agents created")
    public Uni<Response> createAgent(@Valid AgentDefinition agent) {

        LOG.infof("Creating agent: %s", agent.getName());

        // Validate agent
        return validateAgent(agent)
                .chain(valid -> {
                    if (!valid) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.BAD_REQUEST)
                                        .entity(Map.of("error", "Agent validation failed"))
                                        .build());
                    }

                    // Set metadata
                    if (agent.getMetadata() == null) {
                        agent.setMetadata(new AgentDefinition.Metadata());
                    }
                    agent.getMetadata().setCreatedAt(java.time.Instant.now());
                    agent.getMetadata().setUpdatedAt(java.time.Instant.now());

                    // Save
                    return repository.saveAgent(agent)
                            .map(saved -> Response.status(Response.Status.CREATED)
                                    .entity(saved)
                                    .build())
                            .onFailure().recoverWithItem(error -> {
                                LOG.errorf(error, "Failed to create agent: %s", agent.getName());
                                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                        .entity(Map.of("error", error.getMessage()))
                                        .build();
                            });
                });
    }

    /**
     * Get agent by ID
     */
    @GET
    @Path("/{agentId}")
    @ApiKeyAuth
    public Uni<Response> getAgent(@PathParam("agentId") String agentId) {

        return repository.findAgentById(agentId)
                .map(agent -> {
                    if (agent == null) {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", "Agent not found"))
                                .build();
                    }
                    return Response.ok(agent).build();
                });
    }

    /**
     * List agents with pagination and filters
     */
    @GET
    @ApiKeyAuth
    public Uni<Response> listAgents(
            @QueryParam("status") String status,
            @QueryParam("type") String type,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") @Max(100) int size,
            @QueryParam("search") String search) {

        if (search != null && !search.isEmpty()) {
            return repository.searchAgents(search, size)
                    .map(agents -> Response.ok(Map.of(
                            "items", agents,
                            "total", agents.size(),
                            "page", 0,
                            "size", size)).build());
        }

        return repository.findAllAgents(status, type, page, size)
                .map(agents -> Response.ok(Map.of(
                        "items", agents,
                        "page", page,
                        "size", size)).build());
    }

    /**
     * Update agent
     */
    @PUT
    @Path("/{agentId}")
    @ApiKeyAuth
    public Uni<Response> updateAgent(
            @PathParam("agentId") String agentId,
            @Valid AgentDefinition agent) {

        if (!agentId.equals(agent.getId())) {
            return Uni.createFrom().item(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", "Agent ID mismatch"))
                            .build());
        }

        return repository.updateAgent(agent)
                .map(updated -> Response.ok(updated).build())
                .onFailure().recoverWithItem(error -> {
                    if (error instanceof IllegalArgumentException) {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", error.getMessage()))
                                .build();
                    }
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(Map.of("error", error.getMessage()))
                            .build();
                });
    }

    /**
     * Delete agent
     */
    @DELETE
    @Path("/{agentId}")
    @ApiKeyAuth
    public Uni<Response> deleteAgent(@PathParam("agentId") String agentId) {

        return repository.deleteAgent(agentId)
                .map(deleted -> {
                    if (deleted) {
                        return Response.noContent().build();
                    }
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", "Agent not found"))
                            .build();
                });
    }

    /**
     * Execute agent workflow (async)
     */
    @POST
    @Path("/{agentId}/execute")
    @ApiKeyAuth
    @Metered(name = "workflow_executions", description = "Workflow execution rate")
    public Uni<Response> executeAgent(
            @PathParam("agentId") String agentId,
            @Valid ExecutionRequest request) {

        LOG.infof("Executing agent: %s, workflow: %s", agentId, request.workflowName);

        return repository.findAgentById(agentId)
                .chain(agent -> {
                    if (agent == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity(Map.of("error", "Agent not found"))
                                        .build());
                    }

                    // Validate agent is active
                    if (agent.getStatus() != AgentDefinition.AgentStatus.ACTIVE) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.BAD_REQUEST)
                                        .entity(Map.of("error", "Agent is not active"))
                                        .build());
                    }

                    // Find workflow
                    Workflow workflow = findWorkflow(agent, request.workflowName);
                    if (workflow == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.BAD_REQUEST)
                                        .entity(Map.of("error", "Workflow not found"))
                                        .build());
                    }

                    // Create execution context
                    ExecutionContext context = contextManager.createContext();
                    String executionId = context.getExecutionId();

                    // Save execution start
                    return executionRepository.saveExecutionStart(
                            executionId, agentId, workflow.getId(), request.input)
                            .chain(() -> {
                                // Execute workflow asynchronously
                                Uni<WorkflowRuntimeEngine.ExecutionResult> execution = workflowEngine
                                        .executeWorkflow(workflow, request.input, context)
                                        .ifNoItem().after(Duration.ofSeconds(300))
                                        .fail()
                                        .onFailure().recoverWithItem(error -> {
                                            LOG.errorf(error, "Workflow execution failed: %s", executionId);
                                            return new WorkflowRuntimeEngine.ExecutionResult(
                                                    false, null, context.getExecutionTrace(), error.getMessage());
                                        });

                                // Save result when complete (don't block response)
                                execution.subscribe().with(
                                        result -> executionRepository.saveExecutionComplete(executionId, result)
                                                .subscribe().with(
                                                        v -> LOG.infof("Execution saved: %s", executionId),
                                                        err -> LOG.errorf(err, "Failed to save execution: %s",
                                                                executionId)));

                                // Return execution ID immediately
                                return Uni.createFrom().item(
                                        Response.accepted(Map.of(
                                                "executionId", executionId,
                                                "status", "running",
                                                "message", "Workflow execution started")).build());
                            });
                });
    }

    /**
     * Execute with orchestration pattern
     */
    @POST
    @Path("/{agentId}/execute/orchestrated")
    @ApiKeyAuth
    public Uni<Response> executeWithOrchestration(
            @PathParam("agentId") String agentId,
            @Valid OrchestrationExecutionRequest request) {

        return repository.findAgentById(agentId)
                .chain(agent -> {
                    if (agent == null) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND).build());
                    }

                    OrchestrationPattern pattern = request.pattern;
                    if (pattern == null) {
                        pattern = new OrchestrationPattern();
                        pattern.setType(OrchestrationPattern.PatternType.SINGLE_AGENT);
                    }

                    return orchestrationEngine.execute(agent, request.input, pattern)
                            .map(result -> Response.ok(result).build())
                            .onFailure().recoverWithItem(error -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                    .entity(Map.of("error", error.getMessage()))
                                    .build());
                });
    }

    /**
     * Get execution status
     */
    @GET
    @Path("/executions/{executionId}")
    @ApiKeyAuth
    public Uni<Response> getExecutionStatus(@PathParam("executionId") String executionId) {

        // Check in-memory first
        ExecutionContext context = contextManager.getContext(executionId);
        if (context != null) {
            return Uni.createFrom().item(
                    Response.ok(Map.of(
                            "executionId", executionId,
                            "status", "running",
                            "duration", context.getExecutionDuration(),
                            "trace", context.getExecutionTrace())).build());
        }

        // Check database
        return executionRepository.getExecution(executionId)
                .map(execution -> {
                    if (execution == null) {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", "Execution not found"))
                                .build();
                    }

                    Map<String, Object> response = new HashMap<>();
                    response.put("executionId", execution.executionId);
                    response.put("status", execution.status);
                    response.put("startTime", execution.startTime);
                    response.put("endTime", execution.endTime);
                    response.put("duration", execution.durationMs);

                    if (execution.errorMessage != null) {
                        response.put("error", execution.errorMessage);
                    }

                    return Response.ok(response).build();
                });
    }

    /**
     * Get execution history
     */
    @GET
    @Path("/{agentId}/executions")
    @ApiKeyAuth
    public Uni<Response> getExecutionHistory(
            @PathParam("agentId") String agentId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {

        return executionRepository.getExecutionHistory(agentId, page, size)
                .map(executions -> Response.ok(Map.of(
                        "items", executions,
                        "page", page,
                        "size", size)).build());
    }

    /**
     * Stream execution progress (Server-Sent Events)
     */
    @GET
    @Path("/executions/{executionId}/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @ApiKeyAuth
    public Multi<String> streamExecution(@PathParam("executionId") String executionId) {

        return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
                .onItem().transform(tick -> {
                    ExecutionContext context = contextManager.getContext(executionId);
                    if (context == null) {
                        return "data: {\"status\":\"completed\"}\n\n";
                    }

                    Map<String, Object> status = Map.of(
                            "status", "running",
                            "duration", context.getExecutionDuration(),
                            "traceSize", context.getExecutionTrace().size());

                    try {
                        return "data: " + new com.fasterxml.jackson.databind.ObjectMapper()
                                .writeValueAsString(status) + "\n\n";
                    } catch (Exception e) {
                        return "data: {\"error\":\"" + e.getMessage() + "\"}\n\n";
                    }
                })
                .select().first(300); // Max 5 minutes
    }

    /**
     * Cancel execution
     */
    @POST
    @Path("/executions/{executionId}/cancel")
    @ApiKeyAuth
    public Uni<Response> cancelExecution(@PathParam("executionId") String executionId) {

        ExecutionContext context = contextManager.getContext(executionId);
        if (context == null) {
            return Uni.createFrom().item(
                    Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", "Execution not found or already completed"))
                            .build());
        }

        // TODO: Implement cancellation logic
        contextManager.removeContext(executionId);

        return Uni.createFrom().item(
                Response.ok(Map.of("message", "Execution cancelled")).build());
    }

    /**
     * Get agent statistics
     */
    @GET
    @Path("/{agentId}/stats")
    @ApiKeyAuth
    public Uni<Response> getAgentStats(@PathParam("agentId") String agentId) {

        java.time.Instant since = java.time.Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS);

        return executionRepository.getExecutionStats(agentId, since)
                .map(stats -> Response.ok(stats).build());
    }

    // Helper methods

    private Uni<Boolean> validateAgent(AgentDefinition agent) {
        // Validate required fields
        if (agent.getName() == null || agent.getName().isEmpty()) {
            return Uni.createFrom().item(false);
        }
        if (agent.getLlmConfig() == null) {
            return Uni.createFrom().item(false);
        }
        if (agent.getType() == null) {
            return Uni.createFrom().item(false);
        }

        // Validate workflows
        if (agent.getWorkflows() != null) {
            for (Workflow workflow : agent.getWorkflows()) {
                if (!validateWorkflow(workflow)) {
                    return Uni.createFrom().item(false);
                }
            }
        }

        return Uni.createFrom().item(true);
    }

    private boolean validateWorkflow(Workflow workflow) {
        if (workflow.getNodes() == null || workflow.getNodes().isEmpty()) {
            return false;
        }

        // Must have start node
        boolean hasStart = workflow.getNodes().stream()
                .anyMatch(n -> n.getType() == Workflow.Node.NodeType.START);

        return hasStart;
    }

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

    // Request/Response DTOs

    public static class ExecutionRequest {
        @NotNull
        public Map<String, Object> input;

        public String workflowName;
        public Map<String, Object> config;
    }

    public static class OrchestrationExecutionRequest {
        @NotNull
        public Map<String, Object> input;

        public OrchestrationPattern pattern;
    }
}