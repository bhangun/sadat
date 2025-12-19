package tech.kayys.wayang.node.executor;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.model.Workflow;
import tech.kayys.wayang.workflow.model.ExecutionContext;
import tech.kayys.wayang.workflow.service.WorkflowRuntimeEngine.NodeExecutionResult;

import java.util.HashMap;
import java.util.Map;

/**
 * CONDITION node executor
 */

@ApplicationScoped
public class ConditionNodeExecutor implements NodeExecutor {

    @Override
    public Uni<NodeExecutionResult> execute(Workflow.Node node, ExecutionContext context) {
        boolean conditionResult = evaluateCondition(node, context);

        Map<String, Object> output = new HashMap<>();
        output.put("conditionResult", conditionResult);

        return Uni.createFrom().item(
                new NodeExecutionResult(node.getId(), true, output, null));
    }

    private boolean evaluateCondition(Workflow.Node node, ExecutionContext context) {
        Workflow.Node.NodeConfig.Condition condition = node.getConfig().getCondition();
        if (condition == null) {
            return false;
        }

        String expression = condition.getExpression();
        return context.evaluateExpression(expression, context.getAllVariables());
    }

    @Override
    public Workflow.Node.NodeType getSupportedType() {
        return Workflow.Node.NodeType.CONDITION;
    }
}
