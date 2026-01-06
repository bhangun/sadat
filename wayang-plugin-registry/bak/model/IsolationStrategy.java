package tech.kayys.wayang.plugin.runtime.model;

import io.smallrye.mutiny.Uni;
import jakarta.validation.Path.Node;
import tech.kayys.wayang.plugin.runtime.PluginDescriptor;

/**
 * Isolation Strategy Interface
 */
public interface IsolationStrategy {
    Uni<Node> loadPlugin(PluginDescriptor descriptor, byte[] artifactData);
    Uni<Boolean> unloadPlugin(Node instance);
    String getName();
}
