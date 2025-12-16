package tech.kayys.wayang.plugin;


/**
 * Timeout settings
 */
public record TimeoutSettings(
    long executionTimeoutMs,
    long idleTimeoutMs,
    boolean enableTimeout
) {
    public static TimeoutSettings noTimeout() {
        return new TimeoutSettings(0, 0, false);
    }
    
    public static TimeoutSettings defaultTimeout() {
        return new TimeoutSettings(30000, 5000, true);
    }
}