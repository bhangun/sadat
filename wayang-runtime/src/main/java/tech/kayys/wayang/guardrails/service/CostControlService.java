package tech.kayys.wayang.guardrails.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import tech.kayys.wayang.guardrails.dto.GuardrailAction;
import tech.kayys.wayang.guardrails.dto.GuardrailCheckResult;
import tech.kayys.wayang.guardrails.dto.GuardrailSeverity;
import tech.kayys.wayang.guardrails.dto.UsageTracker;

/**
 * Cost control service
 */
@ApplicationScoped
public class CostControlService {

    private static final Logger LOG = LoggerFactory.getLogger(CostControlService.class);

    // user/tenant -> usage tracker
    private final Map<String, UsageTracker> trackers = new ConcurrentHashMap<>();

    public Uni<GuardrailCheckResult> checkTokenBudget(
            String userId,
            String tenantId,
            int estimatedTokens) {
        LOG.debug("Checking token budget for user {} and tenant {}", userId, tenantId);
        return Uni.createFrom().deferred(() -> {
            String key = tenantId + ":" + userId;
            UsageTracker tracker = trackers.computeIfAbsent(
                    key, k -> new UsageTracker(100000)); // Default 100k tokens/day

            if (!tracker.canConsume(estimatedTokens)) {
                return Uni.createFrom().item(GuardrailCheckResult.violation(
                        "cost_control",
                        GuardrailSeverity.HIGH,
                        GuardrailAction.BLOCK,
                        "Token budget exceeded",
                        Map.of("consumed", tracker.consumed,
                                "limit", tracker.dailyLimit)));
            }

            tracker.consume(estimatedTokens);

            return Uni.createFrom().item(GuardrailCheckResult.passed("cost_control"));
        });
    }
}
