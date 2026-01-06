package tech.kayys.wayang.api.node;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import tech.kayys.wayang.api.plugin.TimeoutSettings;
import tech.kayys.wayang.api.execution.RetryPolicy;
import tech.kayys.wayang.api.guardrails.GuardrailsConfig;

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
        TimeoutSettings timeoutSettings,
        Map<String, Object> settings,
        Map<String, String> secrets,
        int timeoutMs,
        int retries,
        GuardrailsConfig guardrailsConfig) {

    public NodeConfig {
        properties = properties != null ? Map.copyOf(properties) : Map.of();
        runtimeSettings = runtimeSettings != null ? Map.copyOf(runtimeSettings) : Map.of();
        guardrailsConfig = guardrailsConfig != null ? guardrailsConfig : GuardrailsConfig.DEFAULT;
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

    private final Map<String, Object> properties;

    public NodeConfig(Map<String, Object> properties) {
        this.properties = properties != null ? properties : Map.of();
    }

    public String getString(String key) {
        return (String) properties.get(key);
    }

    public String getString(String key, String defaultValue) {
        return (String) properties.getOrDefault(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        var value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        var value = properties.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    public double getDouble(String key, double defaultValue) {
        var value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    public <T> T getObject(String key, Class<T> type) {
        return (T) properties.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String key, Class<T> elementType) {
        var value = properties.get(key);
        if (value instanceof List) {
            return (List<T>) value;
        }
        return List.of();
    }
}