package tech.kayys.wayang.workflow.kernel;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.util.Map;

/**
 * Timeout policy for workflow execution.
 */
@Data
@Builder
public class TimeoutPolicy {
    @Builder.Default
    private final Duration overallTimeout = Duration.ofHours(24);

    @Builder.Default
    private final Duration nodeTimeout = Duration.ofMinutes(30);

    private final Map<String, Duration> nodeSpecificTimeouts;
    private final String timeoutAction; // "FAIL", "RETRY", "COMPENSATE"
}
