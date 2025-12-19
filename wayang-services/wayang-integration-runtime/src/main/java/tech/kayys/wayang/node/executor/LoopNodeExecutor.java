package tech.kayys.wayang.node.executor;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.model.Workflow;
import tech.kayys.wayang.agent.model.Workflow.Node.NodeType;
import tech.kayys.wayang.workflow.model.ExecutionContext;
import tech.kayys.wayang.workflow.service.WorkflowRuntimeEngine.NodeExecutionResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class LoopNodeExecutor implements NodeExecutor {

    @Override
    public Uni<NodeExecutionResult> execute(Workflow.Node node, ExecutionContext context) {
        Workflow.Node.NodeConfig.LoopConfig loopConfig = node.getConfig().getLoopConfig();

        if (loopConfig == null) {
            return Uni.createFrom().failure(
                    new IllegalStateException("Loop configuration is required"));
        }

        List<Object> results = new ArrayList<>();
        int maxIterations = loopConfig.getMaxIterations() != null ? loopConfig.getMaxIterations() : 100;

        // Execute loop iterations
        for (int i = 0; i < maxIterations; i++) {
            context.setVariable("loopIndex", i);

            // Check break condition
            if (loopConfig.getBreakCondition() != null) {
                boolean shouldBreak = context.evaluateExpression(
                        loopConfig.getBreakCondition(),
                        context.getAllVariables());
                if (shouldBreak) {
                    break;
                }
            }

            // Add iteration result
            results.add(context.getAllVariables());
        }

        Map<String, Object> output = new HashMap<>();
        output.put("loopResults", results);

        return Uni.createFrom().item(
                new NodeExecutionResult(node.getId(), true, output, null));
    }

    @Override
    public NodeType getSupportedType() {
        return Workflow.Node.NodeType.LOOP;
    }
}