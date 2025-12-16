package tech.kayys.wayang.plugin;

/**
 * Circuit Breaker State
 */
public class CircuitBreakerState {
    private boolean open;
    private int failureCount;

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public static class Builder {
        private boolean open;
        private int failureCount;

        public Builder setOpen(boolean open) {
            this.open = open;
            return this;
        }

        public Builder setFailureCount(int failureCount) {
            this.failureCount = failureCount;
            return this;
        }

        public CircuitBreakerState build() {
            CircuitBreakerState state = new CircuitBreakerState();
            state.setOpen(open);
            state.setFailureCount(failureCount);
            return state;
        }
    }
}

