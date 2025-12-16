package tech.kayys.wayang.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.graphql.Ignore;

/**
 * RuntimeConfig - Runtime execution configuration
 */
public class RuntimeConfig {
    public ExecutionMode mode = ExecutionMode.SYNC;
    public RetryPolicy retryPolicy;
    @Ignore
    public Map<String, Object> sla;
    @Ignore
    public Map<String, Object> telemetry;
    public List<Trigger> triggers = new ArrayList<>();

    public enum ExecutionMode {
        SYNC, ASYNC, STREAM
    }

    public static class RetryPolicy {
        public int maxAttempts = 3;
        public long initialDelayMs = 200;
        public long maxDelayMs = 30000;
        public BackoffStrategy backoff = BackoffStrategy.EXPONENTIAL;
        public boolean jitter = true;

        public enum BackoffStrategy {
            FIXED, EXPONENTIAL, LINEAR
        }
    }

    public static class Trigger {
        public TriggerType type;
        public String expression; // Cron or CEL
        public String path; // Webhook path

        public enum TriggerType {
            CRON, WEBHOOK, MQ, MANUAL, SCHEDULE
        }
    }
}
