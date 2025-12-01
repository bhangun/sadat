package tech.kayys.wayang.node.core.model;

import java.util.Map;
import java.util.Optional;

/**
 * Configuration for a node instance.
 * Contains node-specific properties and runtime settings.
 */
public record NodeConfig(
    String nodeId,
    String instanceId,
    Map<String, Object> properties,
    Map<String, Object> runtimeSettings,
    RetryPolicy retryPolicy,
    TimeoutSettings timeoutSettings
) {
    
    public NodeConfig {
        properties = properties != null ? Map.copyOf(properties) : Map.of();
        runtimeSettings = runtimeSettings != null ? Map.copyOf(runtimeSettings) : Map.of();
    }
    
    /**
     * Get a property value
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProperty(String key, Class<T> type) {
        Object value = properties.get(key);
        if (value == null) {
            return Optional.empty();
        }
        if (type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }
    
    /**
     * Get a property value with default
     */
    public <T> T getProperty(String key, Class<T> type, T defaultValue) {
        return getProperty(key, type).orElse(defaultValue);
    }
}