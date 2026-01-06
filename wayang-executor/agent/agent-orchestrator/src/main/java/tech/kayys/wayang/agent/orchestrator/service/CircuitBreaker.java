package tech.kayys.wayang.agent.orchestrator.service;


/**
 * Simple Circuit Breaker implementation
 */
public class CircuitBreaker {
    
    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreaker.class);
    
    private final String agentId;
    private volatile CircuitState state = CircuitState.CLOSED;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private volatile Instant lastFailureTime;
    
    private static final int FAILURE_THRESHOLD = 5;
    private static final Duration RESET_TIMEOUT = Duration.ofMinutes(1);
    
    public CircuitBreaker(String agentId) {
        this.agentId = agentId;
    }
    
    public <T> Uni<T> call(java.util.function.Supplier<Uni<T>> supplier) {
        if (state == CircuitState.OPEN) {
            // Check if we should attempt reset
            if (shouldAttemptReset()) {
                state = CircuitState.HALF_OPEN;
                LOG.info("Circuit breaker for {} entering HALF_OPEN state", agentId);
            } else {
                return Uni.createFrom().failure(
                    new CircuitBreakerOpenException("Circuit breaker is OPEN for agent: " + agentId)
                );
            }
        }
        
        return supplier.get()
            .onItem().invoke(item -> onSuccess())
            .onFailure().invoke(error -> onFailure());
    }
    
    private void onSuccess() {
        failureCount.set(0);
        if (state == CircuitState.HALF_OPEN) {
            state = CircuitState.CLOSED;
            LOG.info("Circuit breaker for {} now CLOSED", agentId);
        }
    }
    
    private void onFailure() {
        lastFailureTime = Instant.now();
        int failures = failureCount.incrementAndGet();
        
        if (failures >= FAILURE_THRESHOLD) {
            state = CircuitState.OPEN;
            LOG.warn("Circuit breaker for {} now OPEN", agentId);
        }
    }
    
    private boolean shouldAttemptReset() {
        return lastFailureTime != null && 
               Duration.between(lastFailureTime, Instant.now())
                   .compareTo(RESET_TIMEOUT) > 0;
    }
    
    enum CircuitState {
        CLOSED, OPEN, HALF_OPEN
    }
}
