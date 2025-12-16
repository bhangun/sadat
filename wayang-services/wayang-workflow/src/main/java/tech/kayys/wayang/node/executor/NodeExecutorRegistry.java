package tech.kayys.wayang.node.executor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.model.Workflow;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for node executors
 */
@ApplicationScoped
public class NodeExecutorRegistry {

    private final Map<Workflow.Node.NodeType, NodeExecutor> executors = new HashMap<>();

    @Inject
    public NodeExecutorRegistry(Instance<NodeExecutor> executorInstances) {
        executorInstances.forEach(executor -> executors.put(executor.getSupportedType(), executor));
    }

    public NodeExecutor getExecutor(Workflow.Node.NodeType type) {
        NodeExecutor executor = executors.get(type);
        if (executor == null) {
            throw new IllegalArgumentException("No executor found for node type: " + type);
        }
        return executor;
    }
}
