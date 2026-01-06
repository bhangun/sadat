package tech.kayys.wayang.node.service;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.node.dto.NodeDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Loader for node definitions from various sources (plugins, external
 * configurations, etc.)
 */
@ApplicationScoped
public class NodeDefinitionLoader {

    /**
     * Loads node definitions from external plugins or extensions.
     * 
     * @return A list of loaded node definitions.
     */
    public List<NodeDefinition> loadFromPlugins() {
        // TODO: Implement actual plugin loading logic (e.g., using ServiceLoader or
        // classpath scanning)
        // For now, return an empty list as a placeholder
        return new ArrayList<>();
    }
}
