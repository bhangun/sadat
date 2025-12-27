package tech.kayys.wayang.workflow.executor;

import jakarta.enterprise.context.ApplicationScoped;
import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.common.spi.Node;

/**
 * Node factory for creating instances
 */
@ApplicationScoped
class NodeFactory {

    public Uni<Node> create(Class<? extends Node> nodeClass) {
        return Uni.createFrom().item(() -> {
            try {
                return nodeClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to create node instance: " + nodeClass.getName(), e);
            }
        });
    }
}
