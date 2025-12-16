package tech.kayys.wayang.agent.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class AgentRuntimeConfig {

    /**
     * Enable dev UI for agent management
     */
    @ConfigItem(defaultValue = "true")
    public boolean devUiEnabled;

    /**
     * Enable metrics collection
     */
    @ConfigItem(defaultValue = "true")
    public boolean metricsEnabled;

    /**
     * Enable execution tracing
     */
    @ConfigItem(defaultValue = "true")
    public boolean tracingEnabled;

    /**
     * Maximum concurrent workflow executions
     */
    @ConfigItem(defaultValue = "10")
    public int maxConcurrentExecutions;

    /**
     * Execution timeout in seconds
     */
    @ConfigItem(defaultValue = "300")
    public int executionTimeoutSeconds;
}
