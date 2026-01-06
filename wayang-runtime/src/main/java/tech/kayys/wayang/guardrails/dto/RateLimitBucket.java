package tech.kayys.wayang.guardrails.dto;

import java.time.Duration;
import java.time.Instant;

public class RateLimitBucket {
    private final int capacity;
    private final Duration refillInterval;
    private int tokens;
    private Instant lastRefill;

    public RateLimitBucket(RateLimitConfig config) {
        this.capacity = config.requestsPerMinute();
        this.refillInterval = Duration.ofMinutes(1);
        this.tokens = capacity;
        this.lastRefill = Instant.now();
    }

    public synchronized boolean tryConsume() {
        refill();

        if (tokens > 0) {
            tokens--;
            return true;
        }

        return false;
    }

    private void refill() {
        Instant now = Instant.now();
        Duration elapsed = Duration.between(lastRefill, now);

        if (elapsed.compareTo(refillInterval) >= 0) {
            tokens = capacity;
            lastRefill = now;
        }
    }
}
