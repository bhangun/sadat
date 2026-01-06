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
import tech.kayys.wayang.guardrails.dto.RateLimitBucket;
import tech.kayys.wayang.guardrails.dto.RateLimitConfig;

/**
 * Rate limiting service
 */
@ApplicationScoped
public class RateLimitService {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitService.class);

    // user/tenant -> bucket
    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    public Uni<GuardrailCheckResult> check(
            String userId,
            String tenantId,
            RateLimitConfig config) {
        LOG.info("Checking rate limit for user {} and tenant {}", userId, tenantId);
        return Uni.createFrom().deferred(() -> {
            String key = tenantId + ":" + userId;
            RateLimitBucket bucket = buckets.computeIfAbsent(
                    key, k -> new RateLimitBucket(config));

            if (!bucket.tryConsume()) {
                return Uni.createFrom().item(GuardrailCheckResult.violation(
                        "rate_limit",
                        GuardrailSeverity.MEDIUM,
                        GuardrailAction.BLOCK,
                        "Rate limit exceeded",
                        Map.of("limit", config.requestsPerMinute(),
                                "window", "1 minute")));
            }

            return Uni.createFrom().item(GuardrailCheckResult.passed("rate_limit"));
        });
    }
}
