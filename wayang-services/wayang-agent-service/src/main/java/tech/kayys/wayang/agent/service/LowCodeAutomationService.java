package tech.kayys.wayang.agent.service;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.dto.*;
import tech.kayys.wayang.agent.entity.AgentEntity;
import tech.kayys.wayang.agent.entity.ExecutionEntity;
import tech.kayys.wayang.agent.entity.IntegrationEntity;
import tech.kayys.wayang.agent.entity.WorkflowEntity;
import tech.kayys.wayang.agent.repository.AgentEntityRepository;
import tech.kayys.wayang.agent.repository.ExecutionEntityRepository;
import tech.kayys.wayang.agent.repository.IntegrationEntityRepository;
import tech.kayys.wayang.agent.repository.WorkflowEntityRepository;
import tech.kayys.wayang.agent.metrics.MetricsService;
import tech.kayys.wayang.agent.util.LoggingUtil;
import tech.kayys.wayang.agent.audit.AuditService;

import java.util.*;

@ApplicationScoped
public class LowCodeAutomationService {

    @Inject
    AgentEntityRepository agentRepository;

    @Inject
    WorkflowEntityRepository workflowRepository;

    @Inject
    ExecutionEntityRepository executionRepository;

    @Inject
    IntegrationEntityRepository integrationRepository;

    @Inject
    MetricsService metricsService;

    @Inject
    LoggingUtil loggingUtil;

    @Inject
    AuditService auditService;

    public Uni<AgentDefinition> createAgent(AgentWorkflowRequest request) {
        Log.infof("Creating agent: %s for tenant: %s", request.name(), request.tenantId());

        AgentEntity agent = new AgentEntity();
        agent.setName(request.name());
        agent.setDescription(request.description());
        agent.setTenantId(request.tenantId());
        agent.setType(request.agentType());
        agent.setLlmConfig(toJsonString(request.llmConfig()));
        agent.setTools(toJsonString(request.tools()));
        agent.setConfig(toJsonString(Map.of()));

        return agentRepository.persist(agent)
                .onItem().transform(entity -> {
                    // Log the creation
                    loggingUtil.logAgentCreation(request.name(), request.agentType().name(), request.tenantId());
                    metricsService.recordAgentCreated();
                    auditService.auditAgentCreation("system", entity.getId().toString(), request.name(), request.tenantId());
                    return toAgentDefinition(agent);
                });
    }

    public Uni<WorkflowExecution> createWorkflow(AgentWorkflowRequest request) {
        Log.infof("Creating workflow: %s for tenant: %s", request.name(), request.tenantId());

        WorkflowEntity workflow = new WorkflowEntity();
        workflow.setName(request.name());
        workflow.setDescription(request.description());
        workflow.setTenantId(request.tenantId());
        workflow.setExecutionMode(request.nodes() != null || request.edges() != null ?
                ExecutionMode.AUTOMATED : ExecutionMode.MANUAL);
        workflow.setNodesConfig(toJsonString(request.nodes()));
        workflow.setEdgesConfig(toJsonString(request.edges()));

        return workflowRepository.persist(workflow)
                .onItem().transform(entity -> toWorkflowExecution(workflow));
    }

    public Uni<IntegrationDefinition> createIntegration(IntegrationDefinition integrationDef) {
        Log.infof("Creating integration: %s", integrationDef.name());

        IntegrationEntity integration = new IntegrationEntity();
        integration.setName(integrationDef.name());
        integration.setDescription(integrationDef.description());
        integration.setProvider(integrationDef.provider());
        integration.setAuthType(integrationDef.authType());
        integration.setConfig(toJsonString(integrationDef.config()));

        return integrationRepository.persist(integration)
                .onItem().transform(entity -> {
                    metricsService.recordIntegrationCreated();
                    loggingUtil.logIntegrationEvent(integrationDef.name(), "CREATION", true);
                    auditService.auditIntegrationCreation("system", entity.getId().toString(), integrationDef.name(), "default");
                    return toIntegrationDefinition(integration);
                });
    }

    public Uni<AgentExecutionResponse> executeAgent(String agentId, AgentExecutionRequest request) {
        Log.infof("Executing agent: %s", agentId);

        // Record execution start
        metricsService.recordAgentExecutionStarted();

        // Create execution record
        ExecutionEntity execution = new ExecutionEntity();
        execution.setAgentId(agentId);
        execution.setWorkflowId(agentId); // For simplicity, using agentId as workflowId
        execution.setTenantId(request.tenantId());
        execution.setStatus(AgentStatus.PROCESSING);
        execution.setInputs(toJsonString(request.inputs()));

        return executionRepository.persist(execution)
                .onItem().transform(entity -> {
                    // Mock response - in real implementation, this would call actual agent execution
                    AgentExecutionResponse response = new AgentExecutionResponse(
                            execution.getId().toString(),
                            Map.of("result", "Agent execution initiated successfully"),
                            new AgentMetrics(1, 1, 0, 0, 0, 0, 0, 0),
                            Arrays.asList("basic-tool")
                    );

                    // Log execution details
                    loggingUtil.logAgentExecution(agentId, request.tenantId(), request.inputs(), response.outputs());

                    // Record execution completion
                    metricsService.recordAgentExecutionCompleted(true);
                    auditService.auditAgentExecution("system", agentId, request.tenantId(), execution.getId().toString());

                    return response;
                });
    }

    private String toJsonString(Object obj) {
        // Simple JSON conversion - in real app, use Jackson or similar
        if (obj == null) {
            return "{}";
        }
        // For now, returning object's toString, but in real implementation should serialize to JSON
        return obj.toString();
    }

    private AgentDefinition toAgentDefinition(AgentEntity entity) {
        return new AgentDefinition(
                entity.getId().toString(),
                entity.getName(),
                entity.getDescription(),
                entity.getTenantId(),
                entity.getType(),
                null, // Would deserialize from entity.getLlmConfig()
                entity.getTools() != null ? 
                    Arrays.asList(entity.getTools().split(",")) : Collections.emptyList(),
                entity.getConfig() != null ? 
                    Map.of("config", entity.getConfig()) : Map.of(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getIsActive()
        );
    }

    private WorkflowExecution toWorkflowExecution(WorkflowEntity entity) {
        return new WorkflowExecution(
                entity.getId().toString(),
                entity.getId().toString(),
                entity.getTenantId(),
                "CREATED",
                Collections.emptyList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                null
        );
    }

    private IntegrationDefinition toIntegrationDefinition(IntegrationEntity entity) {
        return new IntegrationDefinition(
                entity.getId().toString(),
                entity.getName(),
                entity.getDescription(),
                entity.getProvider(),
                entity.getAuthType(),
                entity.getConfig(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getIsActive()
        );
    }
}