package tech.kayys.wayang.agent.service;

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.exception.CircuitBreakerOpenException;

/**
 * Circuit breaker for LLM provider calls
 */
@ApplicationScoped
public class LLMCircuitBreaker {

    private static final Logger LOG = LoggerFactory.getLogger(LLMCircuitBreaker.class);

    /**
     * Call LLM with circuit breaker protection
     */
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 5000, successThreshold = 3)
    @CircuitBreakerName("llm-provider")
    @Timeout(30000) // 30 seconds
    @Retry(maxRetries = 3, delay = 1000, maxDuration = 60000, jitter = 500)
    @Bulkhead(value = 10, waitingTaskQueue = 20)
    @Fallback(fallbackMethod = "fallbackLLMCall")
    public <T> Uni<T> callWithProtection(java.util.function.Supplier<Uni<T>> call) {
        LOG.debug("Executing LLM call with circuit breaker protection");
        return call.get();
    }

    /**
     * Fallback when circuit is open
     */
    public <T> Uni<T> fallbackLLMCall() {
        LOG.warn("Circuit breaker open - using fallback");
        return Uni.createFrom().failure(
                new CircuitBreakerOpenException("LLM provider circuit breaker is open"));
    }
}
