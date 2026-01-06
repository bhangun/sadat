package tech.kayys.wayang.project.dto;

public class RetryStrategy {
    public int maxAttempts;
    public long initialDelay;
    public double backoffMultiplier;
    public long maxDelay;
}