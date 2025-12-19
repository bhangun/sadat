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
 * MERGE node executor (for parallel branches)
 */
@ApplicationScoped
public class MergeNodeExecutor implements NodeExecutor {

    private static final Logger LOG = Logger.getLogger(MergeNodeExecutor.class);

    @Override
    public Uni<NodeExecutionResult> execute(Workflow.Node node, ExecutionContext context) {
        // Collect all results from parallel branches
        Map<String, Object> mergedOutput = new HashMap<>();

        // Get all variables from context (which should contain parallel results)
        mergedOutput.putAll(context.getAllVariables());

        LOG.debugf("Merged %d values from parallel execution", mergedOutput.size());

        return Uni.createFrom().item(
                new NodeExecutionResult(node.getId(), true, mergedOutput, null));
    }

    @Override
    public NodeType getSupportedType() {
        return Workflow.Node.NodeType.MERGE;
    }
}