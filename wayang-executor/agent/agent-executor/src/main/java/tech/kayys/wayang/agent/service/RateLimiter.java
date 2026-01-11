package tech.kayys.wayang.agent.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.dto.RateLimitStatus;

/**
 * Token Bucket Rate Limiter
 */
@ApplicationScoped
public class RateLimiter {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimiter.class);

    // Configuration per tenant (can be loaded from database)
    private static final int DEFAULT_CAPACITY = 100; // requests
    private static final int DEFAULT_REFILL_RATE = 10; // requests per second

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    /**
     * Check if request is allowed
     */
    public boolean allowRequest(String key) {
        TokenBucket bucket = buckets.computeIfAbsent(
                key,
                k -> new TokenBucket(DEFAULT_CAPACITY, DEFAULT_REFILL_RATE));

        return bucket.tryConsume();
    }

    /**
     * Get rate limit status
     */
    public RateLimitStatus getStatus(String key) {
        TokenBucket bucket = buckets.get(key);
        if (bucket == null) {
            return new RateLimitStatus(DEFAULT_CAPACITY, DEFAULT_CAPACITY, 0);
        }

        return new RateLimitStatus(
                DEFAULT_CAPACITY,
                bucket.getAvailableTokens(),
                bucket.getRequestCount());
    }

    /**
     * Reset rate limit for a key
     */
    public void reset(String key) {
        buckets.remove(key);
        LOG.info("Rate limit reset for: {}", key);
    }

    /**
     * Token Bucket implementation
     */
    private static class TokenBucket {
        private final int capacity;
        private final int refillRate;
        private final AtomicInteger tokens;
        private volatile long lastRefillTimestamp;
        private final AtomicInteger requestCount = new AtomicInteger(0);

        TokenBucket(int capacity, int refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.tokens = new AtomicInteger(capacity);
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        synchronized boolean tryConsume() {
            refill();

            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                requestCount.incrementAndGet();
                return true;
            }

            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTimestamp;

            if (elapsed > 1000) { // 1 second
                int tokensToAdd = (int) (elapsed / 1000) * refillRate;
                int newTokens = Math.min(capacity, tokens.get() + tokensToAdd);
                tokens.set(newTokens);
                lastRefillTimestamp = now;
            }
        }

        int getAvailableTokens() {
            return tokens.get();
        }

        int getRequestCount() {
            return requestCount.get();
        }
    }
}
