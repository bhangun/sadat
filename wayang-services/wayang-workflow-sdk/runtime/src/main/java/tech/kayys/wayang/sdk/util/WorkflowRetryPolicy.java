package tech.kayys.wayang.sdk.util;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;





import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Function;

/**
 * Retry policy utilities for SDK operations
 */
public class WorkflowRetryPolicy {

    private static final Logger log = LoggerFactory.getLogger(WorkflowRetryPolicy.class);

    /**
     * Create exponential backoff retry
     */
    public static <T> Function<Uni<T>, Uni<T>> exponentialBackoff(
        int maxAttempts,
        Duration initialDelay,
        double multiplier
    ) {
        return uni -> uni
            .onFailure().retry()
            .withBackOff(initialDelay, Duration.ofMinutes(5))
            .withJitter(0.5)
            .atMost(maxAttempts)
            .onFailure().invoke(th -> 
                log.error("Retry exhausted after {} attempts", maxAttempts, th)
            );
    }

    /**
     * Create fixed delay retry
     */
    public static <T> Function<Uni<T>, Uni<T>> fixedDelay(
        int maxAttempts,
        Duration delay
    ) {
        return uni -> uni
            .onFailure().retry()
            .withBackOff(delay)
            .atMost(maxAttempts);
    }

    /**
     * Retry only on specific exceptions
     */
    public static <T> Function<Uni<T>, Uni<T>> retryOn(
        Class<? extends Throwable> exceptionClass,
        int maxAttempts,
        Duration delay
    ) {
        return uni -> uni
            .onFailure(exceptionClass).retry()
            .withBackOff(delay)
            .atMost(maxAttempts);
    }
}
