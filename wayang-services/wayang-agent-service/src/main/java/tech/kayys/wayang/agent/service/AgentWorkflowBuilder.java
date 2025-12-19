package tech.kayys.wayang.agent.service;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.dto.AgentWorkflowRequest;
import tech.kayys.wayang.agent.dto.CreateOrchestratorRequest;
import tech.kayys.wayang.agent.dto.AddSubAgentRequest;
import tech.kayys.wayang.agent.dto.WorkflowDefinition;
import tech.kayys.wayang.agent.dto.AgentDefinition;

@ApplicationScoped
public class AgentWorkflowBuilder {

    public Uni<WorkflowDefinition> buildAgentWorkflow(AgentWorkflowRequest request) {
        Log.infof("Building agent workflow for: %s", request.name());

        // This would create a workflow based on the agent request
        // For now, returning a mock workflow definition
        WorkflowDefinition workflow = new WorkflowDefinition(
                java.util.UUID.randomUUID().toString(),
                request.name(),
                request.description(),
                request.tenantId(),
                "ACTIVE",
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                java.util.Map.of("nodes", request.nodes(), "edges", request.edges()));

        return Uni.createFrom().item(workflow);
    }

    public Uni<WorkflowDefinition> buildOrchestratorWorkflow(CreateOrchestratorRequest request) {
        Log.infof("Building orchestrator workflow for: %s", request.name());

        // This would create an orchestrator workflow
        WorkflowDefinition workflow = new WorkflowDefinition(
                java.util.UUID.randomUUID().toString(),
                request.name(),
                request.description(),
                request.tenantId(),
                "ACTIVE",
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                java.util.Map.of("strategy", request.strategy()));

        return Uni.createFrom().item(workflow);
    }

    public Uni<WorkflowDefinition> addSubAgentToOrchestrator(
            AgentDefinition orchestrator,
            AgentDefinition subAgent,
            AddSubAgentRequest request) {

        Log.infof("Adding sub-agent %s to orchestrator %s", subAgent.id(), orchestrator.id());

        // This would update the orchestrator workflow to include the sub-agent
        WorkflowDefinition updatedWorkflow = new WorkflowDefinition(
                orchestrator.id(),
                orchestrator.name(),
                orchestrator.description(),
                orchestrator.tenantId(),
                "ACTIVE",
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                java.util.Map.of("subAgents", java.util.Arrays.asList(subAgent.id())));

        return Uni.createFrom().item(updatedWorkflow);
    }
}