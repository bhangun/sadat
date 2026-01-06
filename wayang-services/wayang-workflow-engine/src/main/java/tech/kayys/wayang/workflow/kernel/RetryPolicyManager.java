package tech.kayys.wayang.workflow.kernel;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.hibernate.engine.config.spi.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.workflow.kernel.WorkflowRunId;
import tech.kayys.wayang.workflow.repository.WorkflowRunRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages retry policies for workflow nodes
 */
@ApplicationScoped
public class RetryPolicyManager {

    private static final Logger LOG = LoggerFactory.getLogger(RetryPolicyManager.class);

    @Inject
    WorkflowRunRepository runRepository;

    @Inject
    ConfigurationService configService;

    private final Map<String, RetryPolicy> policyCache = new ConcurrentHashMap<>();
    private final Map<String, NodeRetryState> retryStates = new ConcurrentHashMap<>();

    public Uni<Boolean> shouldRetry(WorkflowRunId runId, String nodeId, int currentAttempt) {
        return getPolicyForNode(runId, nodeId)
                .map(policy -> {
                    if (policy == null || policy.getMaxAttempts() <= currentAttempt) {
                        return false;
                    }

                    String stateKey = createStateKey(runId.value(), nodeId);
                    NodeRetryState state = retryStates.get(stateKey);

                    if (state == null) {
                        state = new NodeRetryState(nodeId, currentAttempt);
                        retryStates.put(stateKey, state);
                    }

                    // Check if we should back off due to consecutive failures
                    if (state.consecutiveFailures >= policy.getMaxConsecutiveFailures()) {
                        LOG.warn("Max consecutive failures reached for node {} in run {}", nodeId, runId.value());
                        return false;
                    }

                    // Check error classification if available
                    if (state.lastErrorType != null &&
                            policy.getNonRetryableErrors().contains(state.lastErrorType)) {
                        return false;
                    }

                    return true;
                });
    }

    public Uni<Duration> getRetryDelay(WorkflowRunId runId, String nodeId, int currentAttempt) {
        return getPolicyForNode(runId, nodeId)
                .map(policy -> {
                    if (policy == null) {
                        return Duration.ofSeconds(5); // Default
                    }

                    String stateKey = createStateKey(runId.value(), nodeId);
                    NodeRetryState state = retryStates.get(stateKey);

                    if (state == null) {
                        return policy.getInitialDelay();
                    }

                    // Calculate backoff with jitter
                    Duration baseDelay = policy.getInitialDelay();
                    long backoffFactor = (long) Math.pow(policy.getBackoffMultiplier(), currentAttempt - 1);
                    long delayMillis = baseDelay.toMillis() * backoffFactor;

                    // Apply jitter (±20%)
                    double jitter = 0.8 + (Math.random() * 0.4);
                    delayMillis = (long) (delayMillis * jitter);

                    // Cap at max delay
                    if (policy.getMaxDelay() != null) {
                        delayMillis = Math.min(delayMillis, policy.getMaxDelay().toMillis());
                    }

                    return Duration.ofMillis(delayMillis);
                });
    }

    public Uni<Void> recordFailure(WorkflowRunId runId, String nodeId, String errorType) {
        return Uni.createFrom().deferred(() -> {
            String stateKey = createStateKey(runId.value(), nodeId);
            retryStates.compute(stateKey, (key, state) -> {
                if (state == null) {
                    state = new NodeRetryState(nodeId, 1);
                }
                state.recordFailure(errorType);
                state.lastRetryTime = Instant.now();
                return state;
            });

            return Uni.createFrom().voidItem();
        });
    }

    public Uni<Void> recordSuccess(WorkflowRunId runId, String nodeId) {
        return Uni.createFrom().deferred(() -> {
            String stateKey = createStateKey(runId.value(), nodeId);
            retryStates.computeIfPresent(stateKey, (key, state) -> {
                state.resetFailures();
                return state;
            });
            return Uni.createFrom().voidItem();
        });
    }

    private Uni<RetryPolicy> getPolicyForNode(WorkflowRunId runId, String nodeId) {
        String policyKey = runId.value() + ":" + nodeId;

        RetryPolicy cached = policyCache.get(policyKey);
        if (cached != null) {
            return Uni.createFrom().item(cached);
        }

        return runRepository.findById(runId.value())
                .flatMap(run -> configService.getRetryPolicy(
                        run.getWorkflowId(),
                        nodeId,
                        run.getTenantId()))
                .onItem().invoke(policy -> {
                    if (policy != null) {
                        policyCache.put(policyKey, policy);
                    }
                })
                .onFailure().recoverWithNull();
    }

    private String createStateKey(String runId, String nodeId) {
        return runId + ":" + nodeId;
    }

    public static class RetryPolicy {
        private final int maxAttempts;
        private final Duration initialDelay;
        private final double backoffMultiplier;
        private final Duration maxDelay;
        private final int maxConsecutiveFailures;
        private final Set<String> nonRetryableErrors;

        public RetryPolicy(int maxAttempts, Duration initialDelay, double backoffMultiplier,
                Duration maxDelay, int maxConsecutiveFailures, Set<String> nonRetryableErrors) {
            this.maxAttempts = maxAttempts;
            this.initialDelay = initialDelay;
            this.backoffMultiplier = backoffMultiplier;
            this.maxDelay = maxDelay;
            this.maxConsecutiveFailures = maxConsecutiveFailures;
            this.nonRetryableErrors = nonRetryableErrors != null ? new HashSet<>(nonRetryableErrors) : new HashSet<>();
        }

        // Getters...
        public int getMaxAttempts() {
            return maxAttempts;
        }

        public Duration getInitialDelay() {
            return initialDelay;
        }

        public double getBackoffMultiplier() {
            return backoffMultiplier;
        }

        public Duration getMaxDelay() {
            return maxDelay;
        }

        public int getMaxConsecutiveFailures() {
            return maxConsecutiveFailures;
        }

        public Set<String> getNonRetryableErrors() {
            return nonRetryableErrors;
        }
    }

    private static class NodeRetryState {
        final String nodeId;
        int attempt;
        int consecutiveFailures;
        String lastErrorType;
        Instant lastRetryTime;

        NodeRetryState(String nodeId, int attempt) {
            this.nodeId = nodeId;
            this.attempt = attempt;
            this.consecutiveFailures = 0;
        }

        void recordFailure(String errorType) {
            this.attempt++;
            this.consecutiveFailures++;
            this.lastErrorType = errorType;
        }

        void resetFailures() {
            this.consecutiveFailures = 0;
            this.lastErrorType = null;
        }
    }
}