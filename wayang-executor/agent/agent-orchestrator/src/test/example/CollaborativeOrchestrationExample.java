package tech.kayys.wayang.agent.orchestrator.example;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.silat.execution.NodeExecutionTask;
import tech.kayys.wayang.agent.AgentCapability;
import tech.kayys.wayang.agent.AnalyticsAgent;
import tech.kayys.wayang.agent.AnalyticsCapability;
import tech.kayys.wayang.agent.CommonAgent;
import tech.kayys.wayang.agent.CoordinationStrategy;
import tech.kayys.wayang.agent.OrchestrationType;
import tech.kayys.wayang.agent.dto.AgentEndpoint;
import tech.kayys.wayang.agent.dto.AgentExecutionRequest;
import tech.kayys.wayang.agent.dto.AgentRegistration;
import tech.kayys.wayang.agent.dto.EndpointType;
import tech.kayys.wayang.agent.orchestrator.service.AgentOrchestratorExecutor;
import tech.kayys.wayang.agent.orchestrator.service.AgentRegistry;

/**
 * Example 2: Multi-Agent Collaboration
 */
@ApplicationScoped
public class CollaborativeOrchestrationExample {
    
    private static final Logger LOG = LoggerFactory.getLogger(CollaborativeOrchestrationExample.class);
    
    @Inject
    AgentOrchestratorExecutor orchestrator;
    
    @Inject
    AgentRegistry agentRegistry;
    
    /**
     * Example: Multiple agents collaborate to solve complex problem
     */
    public Uni<Map<String, Object>> collaborativeProblemSolving(String problemDescription) {
        LOG.info("Starting collaborative problem solving");
        
        // Register specialized agents for collaboration
        registerSpecializedAgents().await().indefinitely();
        
        // Create orchestration request with collaborative configuration
        AgentExecutionRequest request = AgentExecutionRequest.builder()
            .taskDescription(problemDescription)
            .context("orchestrationType", OrchestrationType.COLLABORATIVE.name())
            .context("coordinationStrategy", CoordinationStrategy.CONSENSUS.name())
            .context("participatingAgents", List.of(
                "research-agent",
                "analysis-agent",
                "synthesis-agent"
            ))
            .requiredCapability("COLLABORATIVE")
            .build();
        
        NodeExecutionTask task = createCollaborativeTask(request);
        
        return orchestrator.execute(task)
            .map(result -> (Map<String, Object>) result.output());
    }
    
    private Uni<Void> registerSpecializedAgents() {
        // Register research agent
        Uni<AgentRegistration> research = agentRegistry.registerAgent(
            "research-agent",
            "Research Specialist",
            new CommonAgent("research", Set.of("web-search", "data-gathering")),
            Set.of(AgentCapability.TOOL_USE, AgentCapability.COLLABORATIVE),
            new AgentEndpoint(EndpointType.GRPC, "localhost:9092", Map.of()),
            "demo-tenant"
        );
        
        // Register analysis agent
        Uni<AgentRegistration> analysis = agentRegistry.registerAgent(
            "analysis-agent",
            "Analysis Specialist",
            new AnalyticsAgent(
                Set.of(AnalyticsCapability.DESCRIPTIVE, AnalyticsCapability.DIAGNOSTIC),
                Set.of("json", "csv"),
                false
            ),
            Set.of(AgentCapability.DATA_ANALYSIS, AgentCapability.COLLABORATIVE),
            new AgentEndpoint(EndpointType.GRPC, "localhost:9093", Map.of()),
            "demo-tenant"
        );
        
        // Register synthesis agent
        Uni<AgentRegistration> synthesis = agentRegistry.registerAgent(
            "synthesis-agent",
            "Synthesis Specialist",
            new CommonAgent("synthesis", Set.of("summarization", "integration")),
            Set.of(AgentCapability.REASONING, AgentCapability.COLLABORATIVE),
            new AgentEndpoint(EndpointType.GRPC, "localhost:9094", Map.of()),
            "demo-tenant"
        );
        
        return Uni.join().all(research, analysis, synthesis)
            .andFailFast()
            .replaceWithVoid();
    }
    
    private NodeExecutionTask createCollaborativeTask(AgentExecutionRequest request) {
        // Similar to createMockTask
        return null;
    }
}