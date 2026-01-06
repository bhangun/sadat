package tech.kayys.wayang.agent.orchestrator.example;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.silat.execution.NodeExecutionTask;
import tech.kayys.wayang.agent.dto.AgentExecutionRequest;
import tech.kayys.wayang.agent.dto.ExecutionConstraints;
import tech.kayys.wayang.agent.orchestrator.service.AgentOrchestratorExecutor;
import tech.kayys.wayang.agent.orchestrator.service.AgentRegistry;

/**
 * Example 1: Simple Task Orchestration
 */
@ApplicationScoped
public class SimpleOrchestrationExample {
    
    private static final Logger LOG = LoggerFactory.getLogger(SimpleOrchestrationExample.class);
    
    @Inject
    AgentOrchestratorExecutor orchestrator;
    
    @Inject
    AgentRegistry agentRegistry;
    
    /**
     * Example: Orchestrate a simple data analysis task
     */
    public Uni<Map<String, Object>> analyzeCustomerData(String customerId) {
        LOG.info("Starting customer data analysis for: {}", customerId);
        
        // Create execution request
        AgentExecutionRequest request = AgentExecutionRequest.builder()
            .taskDescription("Analyze customer purchase history and provide insights")
            .context("customerId", customerId)
            .context("analysisType", "comprehensive")
            .requiredCapability("DATA_ANALYSIS")
            .requiredCapability("REASONING")
            .constraints(new ExecutionConstraints(
                60000L,  // 1 minute timeout
                2,       // max 2 retries
                1073741824L, // 1GB memory
                Set.of("database", "analytics"),
                Map.of()
            ))
            .build();
        
        // Create node execution task (simulate workflow node)
        NodeExecutionTask task = createMockTask(request);
        
        // Execute orchestration
        return orchestrator.execute(task)
            .map(result -> {
                if (result.status().equals(tech.kayys.silat.core.domain.NodeExecutionStatus.COMPLETED)) {
                    LOG.info("Analysis completed successfully");
                    return (Map<String, Object>) result.output().get("output");
                } else {
                    LOG.error("Analysis failed: {}", result.error());
                    throw new RuntimeException("Analysis failed");
                }
            });
    }
    
    private NodeExecutionTask createMockTask(AgentExecutionRequest request) {
        return new NodeExecutionTask(
            tech.kayys.silat.core.domain.WorkflowRunId.generate(),
            tech.kayys.silat.core.domain.NodeId.of("analysis-node"),
            1,
            new tech.kayys.silat.core.domain.ExecutionToken(
                UUID.randomUUID().toString(),
                tech.kayys.silat.core.domain.WorkflowRunId.generate(),
                tech.kayys.silat.core.domain.NodeId.of("analysis-node"),
                1,
                Instant.now().plusSeconds(3600)
            ),
            new HashMap<>(request.context())
        );
    }
}