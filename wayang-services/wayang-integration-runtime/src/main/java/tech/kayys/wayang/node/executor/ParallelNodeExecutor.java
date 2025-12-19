package tech.kayys.wayang.node.executor;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.model.Workflow;
import tech.kayys.wayang.agent.model.Workflow.Node.NodeType;
import tech.kayys.wayang.workflow.model.ExecutionContext;
import tech.kayys.wayang.workflow.service.WorkflowRuntimeEngine.NodeExecutionResult;

/**
 * PARALLEL node executor
 */
@ApplicationScoped
public class ParallelNodeExecutor implements NodeExecutor {

    private static final Logger LOG = Logger.getLogger(ParallelNodeExecutor.class);

    @Override
    public Uni<NodeExecutionResult> execute(Workflow.Node node, ExecutionContext context) {
        // This is handled by the workflow engine itself
        // This executor just passes through

        Map<String, Object> output = new HashMap<>();
        output.put("parallelExecution", true);

        return Uni.createFrom().item(
                new NodeExecutionResult(node.getId(), true, output, null));
    }

    @Override
    public NodeType getSupportedType() {
        return Workflow.Node.NodeType.PARALLEL;
    }
}