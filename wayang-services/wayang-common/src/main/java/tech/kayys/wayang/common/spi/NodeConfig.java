package tech.kayys.wayang.common.spi;

import java.util.Map;

/**
 * NodeConfig: runtime configuration
 */
public record NodeConfig(
  
    Map<String, Object> properties,
    GuardrailsConfig guardrailsConfig,
    TelemetryConfig telemetryConfig,
    ResourceProfile resourceProfile
) {


     public <T> T getProperty(String key, Class<T> type) {
        return type.cast(properties.get(key));
    }
    
    public String getProperty(String key, String defaultValue) {
        Object val = properties.get(key);
        return val != null ? val.toString() : defaultValue;
    }
}
