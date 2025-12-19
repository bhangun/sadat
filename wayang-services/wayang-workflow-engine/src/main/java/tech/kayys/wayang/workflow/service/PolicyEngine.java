package tech.kayys.wayang.workflow.service;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.workflow.model.GuardrailResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PolicyEngine - Business policy evaluation.
 */
@ApplicationScoped
public class PolicyEngine {

    private static final Logger LOG = Logger.getLogger(PolicyEngine.class);

    // For a simplified implementation, we can create our own PolicyConfig
    // instead of depending on non-existent schema class
    public static class PolicyConfig {
        public AccessPolicy accessPolicy;
        public RateLimit rateLimit;

        public static class AccessPolicy {
            public String[] requiredRoles;
        }

        public static class RateLimit {
            public int maxRequests;
            public long timeWindowMs;
        }
    }

    /**
     * Evaluate input policy.
     */
    public Uni<GuardrailResult> evaluateInputPolicy(
            String nodeId,
            Map<String, Object> policyConfig,
            String tenantId,
            String userId) {

        if (policyConfig == null || policyConfig.isEmpty()) {
            LOG.debugf("No policy defined for node %s, allowing", nodeId);
            return Uni.createFrom().item(GuardrailResult.allow());
        }

        // Extract policy configuration
        Map<String, Object> accessPolicy = (Map<String, Object>) policyConfig.get("accessPolicy");
        Map<String, Object> rateLimit = (Map<String, Object>) policyConfig.get("rateLimit");

        // Check access policy (RBAC)
        if (accessPolicy != null) {
            boolean authorized = checkAuthorization(
                    nodeId,
                    tenantId,
                    userId,
                    accessPolicy);

            if (!authorized) {
                LOG.warnf("Access denied for user %s on node %s by policy", userId, nodeId);
                return Uni.createFrom().item(
                        GuardrailResult.block("Access denied by policy"));
            }
        }

        // Check rate limits
        if (rateLimit != null) {
            boolean withinLimit = checkRateLimit(
                    nodeId,
                    tenantId,
                    userId,
                    rateLimit);

            if (!withinLimit) {
                LOG.warnf("Rate limit exceeded for user %s on node %s", userId, nodeId);
                return Uni.createFrom().item(
                        GuardrailResult.block("Rate limit exceeded"));
            }
        }

        return Uni.createFrom().item(GuardrailResult.allow());
    }

    /**
     * Evaluate output policy.
     */
    public Uni<GuardrailResult> evaluateOutputPolicy(
            String nodeId,
            Map<String, Object> policyConfig,
            String tenantId,
            Map<String, Object> output) {

        // Placeholder for output policy checks
        return Uni.createFrom().item(GuardrailResult.allow());
    }

    private boolean checkAuthorization(
            String nodeId,
            String tenantId,
            String userId,
            Map<String, Object> accessPolicy) {

        // In a real implementation, this would check user roles against required roles
        // For now, we'll allow all access
        LOG.debugf("Checking authorization for user %s on node %s in tenant %s", userId, nodeId, tenantId);
        return true; // Allow for now - would implement role/permission checks in real system
    }

    // Track request counts for rate limiting
    private final Map<String, RequestTracker> rateLimitTrackers = new ConcurrentHashMap<>();

    static class RequestTracker {
        int requestCount;
        long windowStart;

        RequestTracker(long windowStart) {
            this.requestCount = 1;
            this.windowStart = windowStart;
        }

        synchronized void addRequest(long currentTime) {
            // Reset window if time has passed
            if (currentTime - windowStart >= 60000) { // Reset every minute as default
                requestCount = 1;
                windowStart = currentTime;
            } else {
                requestCount++;
            }
        }

        boolean checkLimit(int maxRequests, long currentTime) {
            // Reset if window has passed
            if (currentTime - windowStart >= 60000) { // Reset every minute as default
                requestCount = 0;
                windowStart = currentTime;
            }
            return requestCount < maxRequests;
        }
    }

    private boolean checkRateLimit(
            String nodeId,
            String tenantId,
            String userId,
            Map<String, Object> rateLimit) {

        LOG.debugf("Checking rate limit for user %s on node %s in tenant %s", userId, nodeId, tenantId);

        // Extract rate limit parameters
        int maxRequests = ((Number) rateLimit.getOrDefault("maxRequests", 100)).intValue();
        long timeWindowMs = ((Number) rateLimit.getOrDefault("timeWindowMs", 60000L)).longValue();

        // Create a key for the limit (e.g., tenant + user + node)
        String key = tenantId + ":" + userId + ":" + nodeId;

        long currentTime = System.currentTimeMillis();

        RequestTracker tracker = rateLimitTrackers.computeIfAbsent(key, k -> new RequestTracker(currentTime));

        boolean withinLimit = tracker.checkLimit(maxRequests, currentTime);

        if (withinLimit) {
            tracker.addRequest(currentTime);
        }

        return withinLimit;
    }
}