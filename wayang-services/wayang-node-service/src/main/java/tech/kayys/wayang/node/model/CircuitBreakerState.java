package tech.kayys.wayang.workflow.model;

/**
 * Circuit breaker state.
 */
enum CircuitBreakerState {
    CLOSED, // Normal operation
    OPEN, // Failing, block requests
    HALF_OPEN // Testing recovery
}
