package tech.kayys.wayang.workflow.scheduler.dto;

/**
 * CircuitBreaker: Prevents execution of consistently failing schedules.
 */
public class CircuitBreaker {
    private final int failureThreshold;
    private final long timeoutMs;
    private int failureCount;
    private long openedAt;
    private CircuitState state = CircuitState.CLOSED;

    public CircuitBreaker(int failureThreshold, long timeoutMs) {
        this.failureThreshold = failureThreshold;
        this.timeoutMs = timeoutMs;
    }

    public void recordFailure() {
        failureCount++;
        if (failureCount >= failureThreshold && state == CircuitState.CLOSED) {
            open();
        }
    }

    public void recordSuccess() {
        failureCount = 0;
        if (state == CircuitState.HALF_OPEN) {
            close();
        }
    }

    public boolean isOpen() {
        if (state == CircuitState.OPEN) {
            // Check if timeout expired
            if (System.currentTimeMillis() - openedAt > timeoutMs) {
                halfOpen();
                return false;
            }
            return true;
        }
        return false;
    }

    public void open() {
        state = CircuitState.OPEN;
        openedAt = System.currentTimeMillis();
    }

    public void close() {
        state = CircuitState.CLOSED;
        failureCount = 0;
    }

    public void halfOpen() {
        state = CircuitState.HALF_OPEN;
    }

    public enum CircuitState {
        CLOSED, OPEN, HALF_OPEN
    }
}
