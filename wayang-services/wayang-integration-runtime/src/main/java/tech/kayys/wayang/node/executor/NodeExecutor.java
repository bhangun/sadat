package tech.kayys.wayang.node.executor;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.model.Workflow;
import tech.kayys.wayang.workflow.model.ExecutionContext;
import tech.kayys.wayang.workflow.service.WorkflowRuntimeEngine.NodeExecutionResult;

/**
 * Base interface for node executors
 */
public interface NodeExecutor {
    Uni<NodeExecutionResult> execute(Workflow.Node node, ExecutionContext context);

    Workflow.Node.NodeType getSupportedType();
}
