package tech.kayys.wayang.node.executor;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.model.Workflow;
import tech.kayys.wayang.agent.model.Workflow.Node.NodeType;
import tech.kayys.wayang.workflow.model.ExecutionContext;
import tech.kayys.wayang.workflow.service.WorkflowRuntimeEngine.NodeExecutionResult;

/**
 * END node executor
 */
@ApplicationScoped
public class EndNodeExecutor implements NodeExecutor {

    @Override
    public Uni<NodeExecutionResult> execute(Workflow.Node node, ExecutionContext context) {
        return Uni.createFrom().item(
                new NodeExecutionResult(node.getId(), true, context.getAllVariables(), null));
    }

    @Override
    public NodeType getSupportedType() {
        return Workflow.Node.NodeType.END;
    }
}