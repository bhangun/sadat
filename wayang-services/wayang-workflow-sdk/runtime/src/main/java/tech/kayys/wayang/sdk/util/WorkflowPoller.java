package tech.kayys.wayang.sdk.util;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;



import tech.kayys.wayang.sdk.WorkflowRunClient;
import tech.kayys.wayang.sdk.dto.WorkflowRunResponse;
import tech.kayys.wayang.sdk.dto.RunStatus;
import tech.kayys.wayang.sdk.exception.WorkflowTimeoutException;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.function.Predicate;

/**
 * Utility for polling workflow status until a condition is met.
 * 
 * Example:
 * <pre>
 * workflowPoller.pollUntilComplete(runId, Duration.ofMinutes(5))
 *     .subscribe().with(
 *         result -> log.info("Workflow completed: {}", result),
 *         error -> log.error("Polling failed", error)
 *     );
 * </pre>
 */
@ApplicationScoped
public class WorkflowPoller {

    private static final Logger log = LoggerFactory.getLogger(WorkflowPoller.class);
    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(2);

    @Inject
    @RestClient
    WorkflowRunClient workflowClient;

    /**
     * Poll until workflow completes (successfully or with failure)
     */
    public Uni<WorkflowRunResponse> pollUntilComplete(String runId, Duration timeout) {
        return pollUntil(
            runId,
            WorkflowRunResponse::isTerminal,
            timeout,
            DEFAULT_POLL_INTERVAL
        );
    }

    /**
     * Poll until workflow reaches specific status
     */
    public Uni<WorkflowRunResponse> pollUntilStatus(
        String runId,
        RunStatus targetStatus,
        Duration timeout
    ) {
        return pollUntil(
            runId,
            response -> response.status() == targetStatus,
            timeout,
            DEFAULT_POLL_INTERVAL
        );
    }

    /**
     * Poll until custom condition is met
     */
    public Uni<WorkflowRunResponse> pollUntil(
        String runId,
        Predicate<WorkflowRunResponse> condition,
        Duration timeout,
        Duration pollInterval
    ) {
        long startTime = System.currentTimeMillis();
        long timeoutMs = timeout.toMillis();

        return pollRecursive(runId, condition, startTime, timeoutMs, pollInterval);
    }

    private Uni<WorkflowRunResponse> pollRecursive(
        String runId,
        Predicate<WorkflowRunResponse> condition,
        long startTime,
        long timeoutMs,
        Duration pollInterval
    ) {
        return workflowClient.getWorkflowRun(runId)
            .onItem().transformToUni(response -> {
                // Check if condition is met
                if (condition.test(response)) {
                    log.debug("Polling condition met for run {}", runId);
                    return Uni.createFrom().item(response);
                }

                // Check timeout
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed >= timeoutMs) {
                    log.error("Polling timeout for run {} after {}ms", runId, elapsed);
                    return Uni.createFrom().failure(
                        new WorkflowTimeoutException(runId, timeoutMs)
                    );
                }

                // Wait and poll again
                log.trace("Polling run {} - status: {}, elapsed: {}ms", 
                    runId, response.status(), elapsed);
                
                return Uni.createFrom().item(response)
                    .onItem().delayIt().by(pollInterval)
                    .onItem().transformToUni(r -> 
                        pollRecursive(runId, condition, startTime, timeoutMs, pollInterval)
                    );
            });
    }

    /**
     * Poll with exponential backoff
     */
    public Uni<WorkflowRunResponse> pollWithBackoff(
        String runId,
        Predicate<WorkflowRunResponse> condition,
        Duration timeout,
        Duration initialInterval,
        double backoffMultiplier,
        Duration maxInterval
    ) {
        long startTime = System.currentTimeMillis();
        return pollWithBackoffRecursive(
            runId, condition, startTime, timeout.toMillis(),
            initialInterval, backoffMultiplier, maxInterval
        );
    }

    private Uni<WorkflowRunResponse> pollWithBackoffRecursive(
        String runId,
        Predicate<WorkflowRunResponse> condition,
        long startTime,
        long timeoutMs,
        Duration currentInterval,
        double backoffMultiplier,
        Duration maxInterval
    ) {
        return workflowClient.getWorkflowRun(runId)
            .onItem().transformToUni(response -> {
                if (condition.test(response)) {
                    return Uni.createFrom().item(response);
                }

                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed >= timeoutMs) {
                    return Uni.createFrom().failure(
                        new WorkflowTimeoutException(runId, timeoutMs)
                    );
                }

                // Calculate next interval with backoff
                Duration calculatedInterval = Duration.ofMillis(
                    (long) (currentInterval.toMillis() * backoffMultiplier)
                );
                Duration finalNextInterval = calculatedInterval.compareTo(maxInterval) > 0 ? maxInterval : calculatedInterval;

                log.trace("Backoff polling - next check in {}ms", finalNextInterval.toMillis());

                return Uni.createFrom().item(response)
                    .onItem().delayIt().by(finalNextInterval)
                    .onItem().transformToUni(r -> 
                        pollWithBackoffRecursive(
                            runId, condition, startTime, timeoutMs,
                            finalNextInterval, backoffMultiplier, maxInterval
                        )
                    );
            });
    }
}
