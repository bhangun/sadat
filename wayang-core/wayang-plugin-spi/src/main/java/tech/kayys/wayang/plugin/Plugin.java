package tech.kayys.wayang.plugin;

import java.util.Optional;

import io.smallrye.mutiny.Uni;

/**
 * Plugin interface for extending the platform
 */
public interface Plugin {
    /**
     * Plugin metadata
     */
    PluginDescriptor getDescriptor();

    /**
     * Initialize plugin
     */
    default Uni<Void> initialize(PluginContext context) {
        return Uni.createFrom().voidItem();
    }

    /**
     * Shutdown plugin
     */
    void shutdown();

    /**
     * Get plugin components
     */
    <T> Optional<T> getComponent(Class<T> componentType);
}