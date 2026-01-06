package tech.kayys.wayang.agent.orchestrator.example;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.silat.execution.NodeExecutionTask;
import tech.kayys.wayang.agent.OrchestrationType;
import tech.kayys.wayang.agent.dto.AgentExecutionRequest;
import tech.kayys.wayang.agent.dto.ExecutionConstraints;
import tech.kayys.wayang.agent.orchestrator.service.AgentOrchestratorExecutor;

/**
 * Example 4: Hierarchical Orchestration
 */
@ApplicationScoped
public class HierarchicalOrchestrationExample {
    
    private static final Logger LOG = LoggerFactory.getLogger(HierarchicalOrchestrationExample.class);
    
    @Inject
    AgentOrchestratorExecutor orchestrator;
    
    /**
     * Example: Complex task with hierarchical decomposition
     */
    public Uni<Map<String, Object>> complexProjectExecution(String projectDescription) {
        LOG.info("Starting hierarchical orchestration for complex project");
        
        // Create hierarchical orchestration request
        AgentExecutionRequest request = AgentExecutionRequest.builder()
            .taskDescription(projectDescription)
            .context("orchestrationType", OrchestrationType.HIERARCHICAL.name())
            .context("maxDepth", 3)
            .context("enableRecursive", true)
            .requiredCapability("ORCHESTRATION")
            .requiredCapability("PLANNING")
            .constraints(new ExecutionConstraints(
                600000L,  // 10 minutes
                5,
                2147483648L, // 2GB
                Set.of(),
                Map.of("allowSubOrchestration", true)
            ))
            .build();
        
        NodeExecutionTask task = createHierarchicalTask(request);
        
        return orchestrator.execute(task)
            .map(result -> {
                Map<String, Object> output = (Map<String, Object>) result.output();
                
                LOG.info("Hierarchical orchestration completed");
                LOG.info("Total steps executed: {}", 
                    ((Map<String, Object>) output.get("metrics")).get("stepsExecuted"));
                
                return output;
            });
    }
    
    private NodeExecutionTask createHierarchicalTask(AgentExecutionRequest request) {
        return null;
    }
}
