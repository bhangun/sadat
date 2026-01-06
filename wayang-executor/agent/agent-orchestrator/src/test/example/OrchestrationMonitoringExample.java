package tech.kayys.wayang.agent.orchestrator.example;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.silat.execution.NodeExecutionTask;
import tech.kayys.wayang.agent.dto.AgentExecutionRequest;
import tech.kayys.wayang.agent.orchestrator.service.AgentOrchestratorExecutor;

/**
 * Example 7: Real-time Monitoring and Metrics
 */
@ApplicationScoped
public class OrchestrationMonitoringExample {
    
    private static final Logger LOG = LoggerFactory.getLogger(OrchestrationMonitoringExample.class);
    
    @Inject
    AgentOrchestratorExecutor orchestrator;
    
    /**
     * Example: Monitor orchestration progress in real-time
     */
    public Uni<Map<String, Object>> monitoredExecution(String task) {
        LOG.info("Starting monitored execution");
        
        AgentExecutionRequest request = AgentExecutionRequest.builder()
            .taskDescription(task)
            .context("enableMonitoring", true)
            .context("metricsCollectionInterval", 1000) // every second
            .build();
        
        NodeExecutionTask executionTask = createMonitoredTask(request);
        
        return orchestrator.execute(executionTask)
            .onItem().invoke(result -> {
                // Log detailed metrics
                Map<String, Object> output = (Map<String, Object>) result.output();
                @SuppressWarnings("unchecked")
                Map<String, Object> metrics = (Map<String, Object>) output.get("metrics");
                
                LOG.info("=== Orchestration Metrics ===");
                LOG.info("Execution Time: {} ms", metrics.get("executionTimeMs"));
                LOG.info("Tokens Used: {}", metrics.get("tokensUsed"));
                LOG.info("Tool Invocations: {}", metrics.get("toolInvocations"));
                LOG.info("Success Score: {}", metrics.get("successScore"));
                LOG.info("============================");
            })
            .map(result -> (Map<String, Object>) result.output());
    }
    
    private NodeExecutionTask createMonitoredTask(AgentExecutionRequest request) {
        return null;
    }
}
