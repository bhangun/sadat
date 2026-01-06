package tech.kayys.wayang.agent.orchestrator.example;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.silat.execution.NodeExecutionTask;
import tech.kayys.wayang.agent.PlanningStrategy;
import tech.kayys.wayang.agent.dto.AgentExecutionRequest;
import tech.kayys.wayang.agent.orchestrator.service.AgentOrchestratorExecutor;

/**
 * Example 6: Adaptive Replanning on Failure
 */
@ApplicationScoped
public class AdaptiveReplanningExample {
    
    private static final Logger LOG = LoggerFactory.getLogger(AdaptiveReplanningExample.class);
    
    @Inject
    AgentOrchestratorExecutor orchestrator;
    
    /**
     * Example: Orchestration with automatic replanning on failure
     */
    public Uni<Map<String, Object>> resilientExecution(String task) {
        LOG.info("Starting resilient execution with replanning enabled");
        
        AgentExecutionRequest request = AgentExecutionRequest.builder()
            .taskDescription(task)
            .context("enableAdaptivePlanning", true)
            .context("maxReplanAttempts", 3)
            .context("planningStrategy", PlanningStrategy.ADAPTIVE.name())
            .build();
        
        NodeExecutionTask executionTask = createAdaptiveTask(request);
        
        return orchestrator.execute(executionTask)
            .map(result -> {
                Map<String, Object> output = (Map<String, Object>) result.output();
                
                // Check if replanning occurred
                @SuppressWarnings("unchecked")
                List<Object> events = (List<Object>) 
                    ((Map<String, Object>) output.get("metadata")).get("events");
                
                boolean replanned = events.stream()
                    .anyMatch(e -> e.toString().contains("REPLANNING_TRIGGERED"));
                
                if (replanned) {
                    LOG.info("Task completed successfully after replanning");
                } else {
                    LOG.info("Task completed on first attempt");
                }
                
                return output;
            });
    }
    
    private NodeExecutionTask createAdaptiveTask(AgentExecutionRequest request) {
        return null;
    }
}