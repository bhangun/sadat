package tech.kayys.wayang.service;

import java.time.Duration;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryPolicy {
    private static final Logger log = LoggerFactory.getLogger(RetryPolicy.class);
    
    private final int maxAttempts;
    private final Duration initialDelay;
    private final Duration maxDelay;
    private final double backoffMultiplier;
    private final Predicate<Exception> retryableExceptions;
    
    private RetryPolicy(Builder builder) {
        this.maxAttempts = builder.maxAttempts;
        this.initialDelay = builder.initialDelay;
        this.maxDelay = builder.maxDelay;
        this.backoffMultiplier = builder.backoffMultiplier;
        this.retryableExceptions = builder.retryableExceptions;
    }
    
    public <T> T execute(Operation<T> operation) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.execute();
            } catch (Exception e) {
                lastException = e;
                
                if (!retryableExceptions.test(e) || attempt >= maxAttempts) {
                    throw e;
                }
                
                long delayMs = calculateDelay(attempt);
                log.warn("Attempt {} failed, retrying in {}ms: {}", 
                    attempt, delayMs, e.getMessage());
                
                Thread.sleep(delayMs);
            }
        }
        
        throw lastException;
    }
    
    private long calculateDelay(int attempt) {
        long delay = (long) (initialDelay.toMillis() * Math.pow(backoffMultiplier, attempt - 1));
        return Math.min(delay, maxDelay.toMillis());
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private int maxAttempts = 3;
        private Duration initialDelay = Duration.ofSeconds(1);
        private Duration maxDelay = Duration.ofSeconds(30);
        private double backoffMultiplier = 2.0;
        private Predicate<Exception> retryableExceptions = e -> true;
        
        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }
        
        public Builder initialDelay(Duration delay) {
            this.initialDelay = delay;
            return this;
        }
        
        public Builder maxDelay(Duration delay) {
            this.maxDelay = delay;
            return this;
        }
        
        public Builder backoffMultiplier(double multiplier) {
            this.backoffMultiplier = multiplier;
            return this;
        }
        
        public Builder retryOn(Predicate<Exception> predicate) {
            this.retryableExceptions = predicate;
            return this;
        }
        
        public RetryPolicy build() {
            return new RetryPolicy(this);
        }
    }
    
    @FunctionalInterface
    public interface Operation<T> {
        T execute() throws Exception;
    }
}
