package tech.kayys.wayang.workflow.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.schema.node.NodeDefinition;

import java.util.List;
import java.util.Map;

/**
 * NodeRegistry - Management of available node types and their capabilities.
 */
@ApplicationScoped
public class NodeRegistry {

    // Simple stub implementation for now

    public Uni<List<String>> getRegisteredNodeTypes() {
        return Uni.createFrom().item(List.of("http-request", "script", "decision", "fork", "join"));
    }

    public Uni<Map<String, Object>> getNodeTypeSchema(String type) {
        // Return dummy schema
        return Uni.createFrom().item(Map.of(
                "type", type,
                "properties", Map.of("timeout", "integer")));
    }

    public Uni<Boolean> validateNode(NodeDefinition node) {
        // Basic validation
        return Uni.createFrom().item(node != null && node.getType() != null);
    }
}
