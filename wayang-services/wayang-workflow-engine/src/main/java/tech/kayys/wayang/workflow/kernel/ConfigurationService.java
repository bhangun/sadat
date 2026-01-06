package tech.kayys.wayang.workflow.kernel;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration service for workflow system
 */
@ApplicationScoped
public class ConfigurationService {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationService.class);

    @jakarta.inject.Inject
    Config config;

    @ConfigProperty(name = "workflow.metrics.enabled", defaultValue = "true")
    boolean metricsEnabled;

    @ConfigProperty(name = "workflow.metrics.backend", defaultValue = "log")
    String metricsBackend;

    @ConfigProperty(name = "workflow.retry.max-attempts", defaultValue = "3")
    int defaultMaxRetryAttempts;

    @ConfigProperty(name = "workflow.retry.initial-delay", defaultValue = "PT1S")
    Duration defaultRetryInitialDelay;

    @ConfigProperty(name = "workflow.cluster.enabled", defaultValue = "false")
    boolean clusterEnabled;

    public boolean isMetricsPublishingEnabled() {
        return metricsEnabled;
    }

    public String getMetricsBackend() {
        return metricsBackend;
    }

    public String getDefaultKeyId() {
        return getString("workflow.crypto.default-key-id", "default-key");
    }

    public Map<String, Object> getTenantConfig(String tenantId) {
        LOG.debug("Fetching tenant config for tenant {}", tenantId);
        // In real implementation, load from database or config service
        Map<String, Object> config = new HashMap<>();
        config.put("tenantId", tenantId);
        config.put("maxConcurrentRuns", getInt("workflow.tenant." + tenantId + ".max-concurrent-runs", 100));
        config.put("retentionDays", getInt("workflow.tenant." + tenantId + ".retention-days", 30));
        return config;
    }

    public Uni<RetryPolicyManager.RetryPolicy> getRetryPolicy(String workflowId, String nodeId, String tenantId) {
        return Uni.createFrom().deferred(() -> {
            // Load from configuration
            int maxAttempts = getInt(
                    String.format("workflow.%s.node.%s.retry.max-attempts", workflowId, nodeId),
                    defaultMaxRetryAttempts);

            Duration initialDelay = getDuration(
                    String.format("workflow.%s.node.%s.retry.initial-delay", workflowId, nodeId),
                    defaultRetryInitialDelay);

            double backoffMultiplier = getDouble(
                    String.format("workflow.%s.node.%s.retry.backoff-multiplier", workflowId, nodeId),
                    2.0);

            Duration maxDelay = getDuration(
                    String.format("workflow.%s.node.%s.retry.max-delay", workflowId, nodeId),
                    Duration.ofMinutes(5));

            int maxConsecutiveFailures = getInt(
                    String.format("workflow.%s.node.%s.retry.max-consecutive-failures", workflowId, nodeId),
                    5);

            return Uni.createFrom().item(new RetryPolicyManager.RetryPolicy(
                    maxAttempts, initialDelay, backoffMultiplier, maxDelay,
                    maxConsecutiveFailures, null // nonRetryableErrors would come from config
            ));
        });
    }

    public boolean isClusterEnabled() {
        return clusterEnabled;
    }

    public Duration getEventRetention() {
        return getDuration("workflow.events.retention", Duration.ofDays(30));
    }

    public int getSnapshotInterval() {
        return getInt("workflow.snapshot.interval", 100);
    }

    public boolean isEventCompactionEnabled() {
        return getBoolean("workflow.events.compaction.enabled", true);
    }

    private String getString(String key, String defaultValue) {
        Optional<String> value = config.getOptionalValue(key, String.class);
        return value.orElse(defaultValue);
    }

    private int getInt(String key, int defaultValue) {
        Optional<Integer> value = config.getOptionalValue(key, Integer.class);
        return value.orElse(defaultValue);
    }

    private double getDouble(String key, double defaultValue) {
        Optional<Double> value = config.getOptionalValue(key, Double.class);
        return value.orElse(defaultValue);
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        Optional<Boolean> value = config.getOptionalValue(key, Boolean.class);
        return value.orElse(defaultValue);
    }

    private Duration getDuration(String key, Duration defaultValue) {
        Optional<Duration> value = config.getOptionalValue(key, Duration.class);
        return value.orElse(defaultValue);
    }
}