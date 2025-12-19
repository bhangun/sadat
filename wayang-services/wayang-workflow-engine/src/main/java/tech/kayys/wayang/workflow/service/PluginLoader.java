package tech.kayys.wayang.workflow.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.common.spi.Node;

@ApplicationScoped
public class PluginLoader {
    public Uni<Class<? extends Node>> loadNodeClass(String pluginId, String nodeType) {
        // Placeholder implementation
        return Uni.createFrom().failure(new UnsupportedOperationException("Plugin loading not implemented"));
    }

    public Uni<Node> loadNode(String pluginId, String nodeType) {
        // Placeholder implementation
        return Uni.createFrom().failure(new UnsupportedOperationException("Plugin loading not implemented"));
    }

    public Uni<Void> reloadPlugin(String pluginId) {
        // Placeholder implementation
        return Uni.createFrom().failure(new UnsupportedOperationException("Plugin reloading not implemented"));
    }
}
