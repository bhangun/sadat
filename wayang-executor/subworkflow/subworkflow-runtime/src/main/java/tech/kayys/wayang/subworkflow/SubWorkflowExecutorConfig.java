package tech.kayys.silat.executor.subworkflow;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Sub-workflow executor configuration properties
 */
@ApplicationScoped
class SubWorkflowExecutorConfig {

    @ConfigProperty(name = "silat.subworkflow.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "silat.subworkflow.default-timeout-seconds", defaultValue = "3600")
    long defaultTimeoutSeconds;

    @ConfigProperty(name = "silat.subworkflow.max-depth", defaultValue = "10")
    int maxNestingDepth;

    @ConfigProperty(name = "silat.subworkflow.enable-cross-tenant", defaultValue = "false")
    boolean enableCrossTenant;

    @ConfigProperty(name = "silat.subworkflow.poll-interval-ms", defaultValue = "500")
    long pollIntervalMs;

    @ConfigProperty(name = "silat.subworkflow.max-concurrent", defaultValue = "100")
    int maxConcurrent;

    public boolean isEnabled() { return enabled; }
    public long getDefaultTimeoutSeconds() { return defaultTimeoutSeconds; }
    public int getMaxNestingDepth() { return maxNestingDepth; }
    public boolean isEnableCrossTenant() { return enableCrossTenant; }
    public long getPollIntervalMs() { return pollIntervalMs; }
    public int getMaxConcurrent() { return maxConcurrent; }
}