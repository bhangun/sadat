package tech.kayys.wayang.agent.orchestrator.example;

import java.util.ArrayList;
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
import tech.kayys.wayang.agent.CommonAgent;
import tech.kayys.wayang.agent.OrchestrationType;
import tech.kayys.wayang.agent.dto.AgentEndpoint;
import tech.kayys.wayang.agent.dto.AgentExecutionRequest;
import tech.kayys.wayang.agent.dto.AgentRegistration;
import tech.kayys.wayang.agent.dto.EndpointType;
import tech.kayys.wayang.agent.orchestrator.service.AgentOrchestratorExecutor;
import tech.kayys.wayang.agent.orchestrator.service.AgentRegistry;

/**
 * Example 5: Competitive Execution (Best Result Wins)
 */
@ApplicationScoped
public class CompetitiveOrchestrationExample {
    
    private static final Logger LOG = LoggerFactory.getLogger(CompetitiveOrchestrationExample.class);
    
    @Inject
    AgentOrchestratorExecutor orchestrator;
    
    @Inject
    AgentRegistry agentRegistry;
    
    /**
     * Example: Multiple agents compete to provide best solution
     */
    public Uni<Map<String, Object>> competitiveSolution(String challenge) {
        LOG.info("Starting competitive orchestration");
        
        // Register multiple competing agents
        registerCompetingAgents().await().indefinitely();
        
        AgentExecutionRequest request = AgentExecutionRequest.builder()
            .taskDescription(challenge)
            .context("orchestrationType", OrchestrationType.COMPETITIVE.name())
            .context("competingAgents", List.of(
                "agent-approach-1",
                "agent-approach-2",
                "agent-approach-3"
            ))
            .context("selectionCriteria", "highest_quality_score")
            .build();
        
        NodeExecutionTask task = createCompetitiveTask(request);
        
        return orchestrator.execute(task)
            .map(result -> {
                Map<String, Object> output = (Map<String, Object>) result.output();
                
                LOG.info("Winning agent: {}", output.get("winnerAgentId"));
                LOG.info("Winning score: {}", output.get("winningScore"));
                
                return output;
            });
    }
    
    private Uni<Void> registerCompetingAgents() {
        List<Uni<AgentRegistration>> registrations = new ArrayList<>();
        
        for (int i = 1; i <= 3; i++) {
            final int agentNum = i;
            Uni<AgentRegistration> reg = agentRegistry.registerAgent(
                "agent-approach-" + i,
                "Competitive Agent " + i,
                new CommonAgent("approach-" + i, Set.of("problem-solving")),
                Set.of(AgentCapability.REASONING),
                new AgentEndpoint(EndpointType.GRPC, "localhost:909" + (5 + i), Map.of()),
                "demo-tenant"
            );
            registrations.add(reg);
        }
        
        return Uni.join().all(registrations).andFailFast().replaceWithVoid();
    }
    
    private NodeExecutionTask createCompetitiveTask(AgentExecutionRequest request) {
        return null;
    }
}