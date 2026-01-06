package tech.kayys.wayang.agent.orchestrator.service;


/**
 * ============================================================================
 * CIRCUIT BREAKER REGISTRY
 * ============================================================================
 * 
 * Circuit breaker pattern for agent failure isolation
 */
@ApplicationScoped
public class CircuitBreakerRegistry {
    
    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerRegistry.class);
    
    private final ConcurrentMap<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();
    
    public CircuitBreaker getOrCreate(String agentId) {
        return breakers.computeIfAbsent(agentId, id -> {
            LOG.debug("Creating circuit breaker for agent: {}", id);
            return new CircuitBreaker(id);
        });
    }
    
    public void remove(String agentId) {
        breakers.remove(agentId);
    }
}
