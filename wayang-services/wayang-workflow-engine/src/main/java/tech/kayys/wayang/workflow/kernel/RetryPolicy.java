package tech.kayys.wayang.workflow.kernel;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.util.Map;

/**
 * Retry policy for workflow execution.
 */
@Data
@Builder
public class RetryPolicy {
    @Builder.Default
    private final int maxAttempts = 3;

    @Builder.Default
    private final Duration initialDelay = Duration.ofSeconds(1);

    @Builder.Default
    private final double backoffMultiplier = 2.0;

    @Builder.Default
    private final Duration maxDelay = Duration.ofMinutes(5);

    private final Map<String, Object> conditions; // When to retry
}
