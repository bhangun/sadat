package tech.kayys.wayang.mcp.security;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Rate limiter
 */
@ApplicationScoped
public class RateLimiter {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimiter.class);

    private final Map<String, RateLimitBucket> buckets = new java.util.concurrent.ConcurrentHashMap<>();

    public void checkLimit(
            String tenantId,
            String toolId,
            tech.kayys.wayang.mcp.domain.ToolGuardrails guardrails) {

        String key = tenantId + ":" + toolId;
        RateLimitBucket bucket = buckets.computeIfAbsent(
                key,
                k -> new RateLimitBucket(
                        guardrails.getRateLimitPerMinute(),
                        guardrails.getRateLimitPerHour()));

        if (!bucket.tryAcquire()) {
            throw new tech.kayys.wayang.mcp.runtime.RateLimitExceededException(
                    "Rate limit exceeded for tool: " + toolId);
        }
    }

    private static class RateLimitBucket {
        private final int perMinute;
        private final int perHour;
        private int minuteCount = 0;
        private int hourCount = 0;
        private long lastMinuteReset = System.currentTimeMillis();
        private long lastHourReset = System.currentTimeMillis();

        RateLimitBucket(int perMinute, int perHour) {
            this.perMinute = perMinute;
            this.perHour = perHour;
        }

        synchronized boolean tryAcquire() {
            long now = System.currentTimeMillis();

            // Reset counters if needed
            if (now - lastMinuteReset > 60000) {
                minuteCount = 0;
                lastMinuteReset = now;
            }
            if (now - lastHourReset > 3600000) {
                hourCount = 0;
                lastHourReset = now;
            }

            // Check limits
            if (minuteCount >= perMinute || hourCount >= perHour) {
                return false;
            }

            minuteCount++;
            hourCount++;
            return true;
        }
    }
}