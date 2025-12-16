package tech.kayys.wayang.model;

/**
 * Retry policy configuration
 */
record RetryPolicy(
        int maxAttempts,
        long initialDelayMs,
        long maxDelayMs,
        double backoffMultiplier,
        boolean exponentialBackoff) {
    public static RetryPolicy noRetry() {
        return new RetryPolicy(1, 0, 0, 1.0, false);
    }

    public static RetryPolicy defaultPolicy() {
        return new RetryPolicy(3, 100, 5000, 2.0, true);
    }
}