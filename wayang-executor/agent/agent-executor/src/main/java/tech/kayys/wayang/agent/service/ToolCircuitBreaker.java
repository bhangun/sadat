package tech.kayys.wayang.agent.service;

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Circuit breaker for tool executions
 */
@ApplicationScoped
public class ToolCircuitBreaker {

    private static final Logger LOG = LoggerFactory.getLogger(ToolCircuitBreaker.class);

    /**
     * Execute tool with circuit breaker protection
     */
    @CircuitBreaker(requestVolumeThreshold = 5, failureRatio = 0.6, delay = 3000, successThreshold = 2)
    @CircuitBreakerName("tool-execution")
    @Timeout(15000) // 15 seconds
    @Retry(maxRetries = 2, delay = 500, maxDuration = 30000)
    @Bulkhead(value = 5, waitingTaskQueue = 10)
    public <T> Uni<T> executeWithProtection(
            String toolName,
            java.util.function.Supplier<Uni<T>> execution) {

        LOG.debug("Executing tool with circuit breaker: {}", toolName);
        return execution.get();
    }
}
