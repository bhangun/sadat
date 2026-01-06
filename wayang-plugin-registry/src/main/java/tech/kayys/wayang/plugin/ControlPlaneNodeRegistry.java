package tech.kayys.wayang.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.plugin.node.NodeDefinition;

/**
 * Control Plane Node Registry - Authority for node definitions
 */
@ApplicationScoped
public class ControlPlaneNodeRegistry {

    private static final Logger LOG = Logger.getLogger(ControlPlaneNodeRegistry.class);

    private final Map<String, NodeDefinition> nodeRegistry = new ConcurrentHashMap<>();

    @Inject
    SchemaValidator schemaValidator;

    /**
     * Register node with schema validation
     */
    public void register(NodeDefinition node) {
        // Validate schemas are proper JSON Schema
        validateSchemas(node);

        nodeRegistry.put(node.type, node);
        LOG.infof("Registered node: %s (executor: %s, protocol: %s)",
                node.type,
                node.executorBinding.executorId,
                node.executorBinding.protocol);
    }

    public void unregister(String nodeType) {
        nodeRegistry.remove(nodeType);
        LOG.infof("Unregistered node: %s", nodeType);
    }

    public NodeDefinition get(String nodeType) {
        return nodeRegistry.get(nodeType);
    }

    public List<NodeDefinition> getAll() {
        return new ArrayList<>(nodeRegistry.values());
    }

    public List<NodeDefinition> getByCategory(String category) {
        return nodeRegistry.values().stream()
                .filter(n -> category.equals(n.category))
                .toList();
    }

    /**
     * Validate node configuration at runtime
     */
    public ValidationResult validateConfig(String nodeType, Map<String, Object> config) {
        NodeDefinition node = nodeRegistry.get(nodeType);
        if (node == null) {
            return ValidationResult.failure("Node type not found: " + nodeType);
        }

        return schemaValidator.validate(node.configSchema, config);
    }

    /**
     * Validate node inputs at runtime
     */
    public ValidationResult validateInputs(String nodeType, Map<String, Object> inputs) {
        NodeDefinition node = nodeRegistry.get(nodeType);
        if (node == null) {
            return ValidationResult.failure("Node type not found: " + nodeType);
        }

        return schemaValidator.validate(node.inputSchema, inputs);
    }

    private void validateSchemas(NodeDefinition node) {
        if (node.configSchema == null && node.inputSchema == null && node.outputSchema == null) {
            LOG.warnf("Node %s has no schemas defined", node.type);
        }
    }
}
