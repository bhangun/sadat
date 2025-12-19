package tech.kayys.wayang.workflow.service;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.schema.node.NodeDefinition;

public interface NodeExecutor {
    Uni<NodeExecutionResult> execute(NodeDefinition nodeDef, NodeContext context);
}
