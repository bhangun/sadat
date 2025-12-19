package tech.kayys.wayang.node.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.model.Workflow;
import tech.kayys.wayang.agent.model.Workflow.Node.NodeType;
import tech.kayys.wayang.tool.service.ToolService;
import tech.kayys.wayang.workflow.model.ExecutionContext;
import tech.kayys.wayang.workflow.service.WorkflowRuntimeEngine.NodeExecutionResult;

import java.util.HashMap;
import java.util.Map;

import javax.tools.Tool;

@ApplicationScoped
public class ToolNodeExecutor implements NodeExecutor {

    @Inject
    ToolService toolService;

    @Override
    public Uni<NodeExecutionResult> execute(Workflow.Node node, ExecutionContext context) {
        Tool tool = node.getConfig().getTool();
        Map<String, Object> parameters = extractParameters(node, context);

        return toolService.executeTool(tool, parameters, context)
                .map(result -> {
                    Map<String, Object> output = new HashMap<>();
                    output.put("toolResult", result);
                    return new NodeExecutionResult(node.getId(), true, output, null);
                });
    }

    private Map<String, Object> extractParameters(Workflow.Node node, ExecutionContext context) {
        Map<String, Object> parameters = new HashMap<>();
        if (node.getInputs() != null) {
            node.getInputs().forEach(input -> {
                Object value = context.resolveVariable(input.getSource());
                parameters.put(input.getName(), value);
            });
        }
        return parameters;
    }

    @Override
    public NodeType getSupportedType() {
        return Workflow.Node.NodeType.TOOL;
    }
}
