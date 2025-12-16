package tech.kayys.wayang.node.service;

/**
 * TRANSFORM node executor
 */
package tech.kayys.wayang.node.executor;

import io.quarkus.ai.agent.runtime.executor.NodeExecutor;
import io.quarkus.ai.agent.runtime.model.Workflow;
import io.quarkus.ai.agent.runtime.context.ExecutionContext;
import io.quarkus.ai.agent.runtime.service.TransformService;
import io.quarkus.ai.agent.runtime.engine.WorkflowRuntimeEngine.NodeExecutionResult;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;

@ApplicationScoped
public class TransformNodeExecutor implements NodeExecutor {

    @Inject
    TransformService transformService;

    @Override
    public Uni<NodeExecutionResult> execute(Workflow.Node node, ExecutionContext context) {
        Workflow.Node.NodeConfig.TransformConfig config = node.getConfig().getTransformConfig();
        Map<String, Object> input = context.getAllVariables();

        return transformService.transform(config, input)
                .map(output -> new NodeExecutionResult(node.getId(), true, output, null));
    }

    @Override
    public NodeType getSupportedType() {
        return Workflow.Node.NodeType.TRANSFORM;
    }
}
