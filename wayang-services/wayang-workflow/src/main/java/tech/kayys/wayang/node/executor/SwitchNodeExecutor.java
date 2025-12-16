package tech.kayys.wayang.node.executor;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.model.Workflow;
import tech.kayys.wayang.agent.model.Workflow.Node.NodeType;
import tech.kayys.wayang.workflow.model.ExecutionContext;
import tech.kayys.wayang.workflow.service.WorkflowRuntimeEngine.NodeExecutionResult;

import org.jboss.logging.Logger;

import java.util.*;

@ApplicationScoped
public class SwitchNodeExecutor implements NodeExecutor {

    private static final Logger LOG = Logger.getLogger(SwitchNodeExecutor.class);

    @Override
    public Uni<NodeExecutionResult> execute(Workflow.Node node, ExecutionContext context) {
        // Switch works like a multi-way condition
        String switchValue = evaluateSwitchExpression(node, context);

        Map<String, Object> output = new HashMap<>();
        output.put("switchValue", switchValue);
        output.put("selectedCase", switchValue);

        LOG.debugf("Switch evaluated to: %s", switchValue);

        return Uni.createFrom().item(
                new NodeExecutionResult(node.getId(), true, output, null));
    }

    private String evaluateSwitchExpression(Workflow.Node node, ExecutionContext context) {
        if (node.getConfig() == null || node.getConfig().getCondition() == null) {
            return "default";
        }

        String expression = node.getConfig().getCondition().getExpression();
        Object result = context.evaluateExpression(expression, context.getAllVariables());

        return result != null ? result.toString() : "default";
    }

    @Override
    public NodeType getSupportedType() {
        return Workflow.Node.NodeType.SWITCH;
    }
}
