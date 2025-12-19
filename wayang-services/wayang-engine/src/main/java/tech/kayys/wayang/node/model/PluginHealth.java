package tech.kayys.wayang.node.model;

import java.time.Instant;
import java.util.Map;

/**
 * Plugin health status.
 */
@lombok.Data
@lombok.Builder
class PluginHealth {
    private String pluginId;
    private String status;
    private Instant loadedAt;
    private Instant lastCheck;
    private Map<String, Object> metrics;

    public static PluginHealth notFound() {
        return PluginHealth.builder()
                .status("NOT_FOUND")
                .build();
    }
}
