package tech.kayys.wayang.node.model;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Node factory for creating instances.
 */
@ApplicationScoped
class NodeFactory {

    public Uni<Node> create(Class<? extends Node> nodeClass) {
        return Uni.createFrom().item(() -> {
            try {
                return nodeClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create node instance", e);
            }
        });
    }
}