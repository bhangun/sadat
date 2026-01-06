package tech.kayys.wayang.agent.orchestrator.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.dto.AgentExecutionRequest;
import tech.kayys.wayang.agent.dto.AgentExecutionResult;
import tech.kayys.wayang.agent.dto.AgentRegistration;
import tech.kayys.wayang.agent.dto.ErrorSeverity;
import tech.kayys.wayang.agent.dto.ExecutionError;
import tech.kayys.wayang.agent.dto.ExecutionMetrics;
import tech.kayys.wayang.agent.dto.ExecutionStatus;

/**
 * ============================================================================
 * ORCHESTRATOR EXECUTION ENGINE
 * ============================================================================
 * 
 * Handles actual agent execution with:
 * - Circuit breaker pattern for fault tolerance
 * - Timeout management
 * - Result caching
 * - Execution monitoring
 */
@ApplicationScoped
public class OrchestratorExecutionEngine {
    
    private static final Logger LOG = LoggerFactory.getLogger(OrchestratorExecutionEngine.class);
    
    @Inject
    AgentCommunicationBus communicationBus;
    
    @Inject
    CircuitBreakerRegistry circuitBreakerRegistry;
    
    // Result cache for idempotency
    private final Map<String, AgentExecutionResult> resultCache = new ConcurrentHashMap<>();
    
    /**
     * Execute task with specific agent
     */
    public Uni<AgentExecutionResult> executeWithAgent(
            AgentRegistration agent,
            AgentExecutionRequest request) {
        
        LOG.debug("Executing with agent: {}", agent.agentId());
        
        // Check cache first
        String cacheKey = buildCacheKey(agent.agentId(), request.requestId());
        AgentExecutionResult cached = resultCache.get(cacheKey);
        if (cached != null) {
            LOG.debug("Returning cached result");
            return Uni.createFrom().item(cached);
        }
        
        // Get circuit breaker for agent
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.getOrCreate(agent.agentId());
        
        // Execute with circuit breaker and timeout
        return circuitBreaker.call(() ->
            communicationBus.sendRequest(agent, request)
                .ifNoItem().after(Duration.ofMillis(request.constraints().maxExecutionTimeMs()))
                .failWith(() -> new TimeoutException("Agent execution timeout"))
        )
        .onItem().invoke(result -> {
            // Cache result
            resultCache.put(cacheKey, result);
            
            // Update agent metrics
            updateAgentMetrics(agent, result);
        })
        .onFailure().retry().atMost(request.constraints().maxRetries())
        .onFailure().recoverWithItem(error -> {
            LOG.error("Agent execution failed after retries", error);
            
            return new AgentExecutionResult(
                request.requestId(),
                agent.agentId(),
                ExecutionStatus.FAILED,
                null,
                List.of(),
                ExecutionMetrics.empty(),
                List.of(new ExecutionError(
                    "EXECUTION_FAILED",
                    error.getMessage(),
                    ErrorSeverity.HIGH,
                    agent.agentId(),
                    Instant.now(),
                    Map.of()
                )),
                Map.of(),
                Instant.now()
            );
        });
    }
    
    private String buildCacheKey(String agentId, String requestId) {
        return agentId + ":" + requestId;
    }
    
    private void updateAgentMetrics(AgentRegistration agent, AgentExecutionResult result) {
        // Update agent's success rate, avg response time, etc.
        // This would update the agent registry
    }
}

