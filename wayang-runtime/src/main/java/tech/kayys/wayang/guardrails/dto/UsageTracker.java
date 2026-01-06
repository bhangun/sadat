package tech.kayys.wayang.guardrails.dto;

import java.time.Duration;
import java.time.Instant;

public class UsageTracker {
    public final int dailyLimit;
    public int consumed;
    public Instant resetTime;

    public UsageTracker(int dailyLimit) {
        this.dailyLimit = dailyLimit;
        this.consumed = 0;
        this.resetTime = Instant.now().plus(Duration.ofDays(1));
    }

    public synchronized boolean canConsume(int tokens) {
        resetIfNeeded();
        return (consumed + tokens) <= dailyLimit;
    }

    public synchronized void consume(int tokens) {
        resetIfNeeded();
        consumed += tokens;
    }

    private void resetIfNeeded() {
        if (Instant.now().isAfter(resetTime)) {
            consumed = 0;
            resetTime = Instant.now().plus(Duration.ofDays(1));
        }
    }
}
