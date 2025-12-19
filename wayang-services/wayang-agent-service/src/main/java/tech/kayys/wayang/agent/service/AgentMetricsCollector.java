package tech.kayys.wayang.agent.service;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.dto.AgentMetrics;
import tech.kayys.wayang.agent.dto.AgentType;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class AgentMetricsCollector {

    private final Map<String, AgentMetrics> agentMetrics = new ConcurrentHashMap<>();
    private final AtomicLong totalExecutions = new AtomicLong(0);
    private final AtomicLong totalAgentsCreated = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);

    public Uni<Void> recordAgentCreated(AgentType agentType) {
        totalAgentsCreated.incrementAndGet();
        Log.infof("Agent created: %s, Total: %d", agentType, totalAgentsCreated.get());
        return Uni.createFrom().voidItem();
    }

    public Uni<Void> recordExecution(String agentId, tech.kayys.wayang.agent.dto.ExecutionMode executionMode) {
        totalExecutions.incrementAndGet();
        Log.infof("Execution recorded for agent: %s, Mode: %s, Total: %d", 
                  agentId, executionMode, totalExecutions.get());
        
        // Update agent-specific metrics
        AgentMetrics metrics = agentMetrics.computeIfAbsent(agentId, k -> 
            new AgentMetrics(0, 0, 0, 0, 0, 0, 0, 0)
        );
        
        // Update the metrics (simplified - in real app would be more detailed)
        agentMetrics.put(agentId, new AgentMetrics(
            metrics.totalExecutions() + 1,
            metrics.successfulExecutions(),
            metrics.failedExecutions(),
            metrics.avgExecutionTimeMs(),
            metrics.maxExecutionTimeMs(),
            metrics.minExecutionTimeMs(),
            metrics.totalTokensUsed(),
            metrics.totalCost()
        ));
        
        return Uni.createFrom().voidItem();
    }

    public Uni<AgentMetrics> getAgentMetrics(String agentId) {
        AgentMetrics metrics = agentMetrics.getOrDefault(agentId, 
            new AgentMetrics(0, 0, 0, 0, 0, 0, 0, 0));
        Log.infof("Retrieved metrics for agent: %s", agentId);
        return Uni.createFrom().item(metrics);
    }

    public Uni<Long> getTotalExecutions() {
        return Uni.createFrom().item(totalExecutions.get());
    }

    public Uni<Long> getTotalAgentsCreated() {
        return Uni.createFrom().item(totalAgentsCreated.get());
    }

    public Uni<Void> recordError(String agentId, String errorMessage) {
        totalErrors.incrementAndGet();
        Log.errorf("Error recorded for agent %s: %s", agentId, errorMessage);
        
        // Update agent metrics
        AgentMetrics metrics = agentMetrics.computeIfAbsent(agentId, k -> 
            new AgentMetrics(0, 0, 0, 0, 0, 0, 0, 0)
        );
        
        agentMetrics.put(agentId, new AgentMetrics(
            metrics.totalExecutions(),
            metrics.successfulExecutions(),
            metrics.failedExecutions() + 1,
            metrics.avgExecutionTimeMs(),
            metrics.maxExecutionTimeMs(),
            metrics.minExecutionTimeMs(),
            metrics.totalTokensUsed(),
            metrics.totalCost()
        ));
        
        return Uni.createFrom().voidItem();
    }

    public Uni<AgentMetrics> getOverallMetrics() {
        AgentMetrics overall = new AgentMetrics(
            totalExecutions.get(),
            totalExecutions.get() - totalErrors.get(), // assuming all non-errors are successful
            totalErrors.get(),
            0, // avg execution time
            0, // max execution time
            0, // min execution time
            0, // total tokens
            0.0 // total cost
        );
        
        Log.info("Retrieved overall metrics");
        return Uni.createFrom().item(overall);
    }

    public Uni<Void> recordExecutionTime(String agentId, long executionTimeMs) {
        AgentMetrics metrics = agentMetrics.computeIfAbsent(agentId, k -> 
            new AgentMetrics(0, 0, 0, 0, 0, 0, 0, 0)
        );
        
        // In a real implementation, we'd calculate proper averages and min/max
        agentMetrics.put(agentId, new AgentMetrics(
            metrics.totalExecutions(),
            metrics.successfulExecutions(),
            metrics.failedExecutions(),
            executionTimeMs, // simplified
            Math.max(metrics.maxExecutionTimeMs(), executionTimeMs),
            metrics.minExecutionTimeMs() == 0 ? executionTimeMs : Math.min(metrics.minExecutionTimeMs(), executionTimeMs),
            metrics.totalTokensUsed(),
            metrics.totalCost()
        ));
        
        return Uni.createFrom().voidItem();
    }
}