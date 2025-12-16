package tech.kayys.wayang.plugin;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.plugin.error.ErrorPayload;

/**
 * Retry Manager
 */
class RetryManager {
    public long calculateBackoff(ErrorPayload error) {
        return 1000L * (long) Math.pow(2, error.getAttempt());
    }
    
    public Uni<Void> scheduleRetry(ErrorPayload error, long delayMs) {
        return Uni.createFrom().voidItem();
    }
}