package tech.kayys.wayang.node.executor;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.model.Workflow;
import tech.kayys.wayang.agent.model.Workflow.Node.NodeType;
import tech.kayys.wayang.workflow.model.ExecutionContext;
import tech.kayys.wayang.workflow.service.WorkflowRuntimeEngine.NodeExecutionResult;

import java.util.HashMap;
import java.util.Map;

/**
 * START node executor
 */
@ApplicationScoped
public class StartNodeExecutor implements NodeExecutor {

    @Override
    public Uni<NodeExecutionResult> execute(Workflow.Node node, ExecutionContext context) {
        Map<String, Object> output = new HashMap<>(context.getInput());
        return Uni.createFrom().item(
                new NodeExecutionResult(node.getId(), true, output, null));
    }

    @Override
    public NodeType getSupportedType() {
        return Workflow.Node.NodeType.START;
    }
}
