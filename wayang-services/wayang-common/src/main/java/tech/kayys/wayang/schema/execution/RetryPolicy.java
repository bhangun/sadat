package tech.kayys.wayang.schema.execution;

import java.util.Arrays;
import java.util.List;

public class RetryPolicy {
    private Integer maxAttempts = 3;
    private String backoff = "exponential";
    private Integer initialDelayMs = 500;
    private Integer maxDelayMs = 30000;
    private Boolean jitter = true;

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        if (maxAttempts != null && (maxAttempts < 0 || maxAttempts > 20)) {
            throw new IllegalArgumentException("Max attempts must be between 0 and 20");
        }
        this.maxAttempts = maxAttempts;
    }

    public String getBackoff() {
        return backoff;
    }

    public void setBackoff(String backoff) {
        List<String> validBackoff = Arrays.asList("fixed", "exponential", "linear");
        if (!validBackoff.contains(backoff)) {
            throw new IllegalArgumentException("Invalid backoff strategy: " + backoff);
        }
        this.backoff = backoff;
    }

    public Integer getInitialDelayMs() {
        return initialDelayMs;
    }

    public void setInitialDelayMs(Integer initialDelayMs) {
        if (initialDelayMs != null && initialDelayMs < 100) {
            throw new IllegalArgumentException("Initial delay must be at least 100ms");
        }
        this.initialDelayMs = initialDelayMs;
    }

    public Integer getMaxDelayMs() {
        return maxDelayMs;
    }

    public void setMaxDelayMs(Integer maxDelayMs) {
        if (maxDelayMs != null && maxDelayMs < 0) {
            throw new IllegalArgumentException("Max delay cannot be negative");
        }
        this.maxDelayMs = maxDelayMs;
    }

    public Boolean getJitter() {
        return jitter;
    }

    public void setJitter(Boolean jitter) {
        this.jitter = jitter;
    }
}
