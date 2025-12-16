package tech.kayys.wayang.plugin.node;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.plugin.ExecutionResult;

/**
 * 
 * Base class for workflow-control nodes.
 * 
 * <p>
 * System nodes manipulate workflow behavior, routing,
 * 
 * parallelism, state handling, and structural orchestration.
 * These nodes do not interact with external systems or AI models
 */
public abstract class SystemNode extends AbstractNode {

    @Override
    protected final Uni<ExecutionResult> doExecute(NodeContext context) {
        return executeSystem(context);
    }

    /**
     * 
     * Core workflow-control logic for system nodes.
     * 
     * @param context execution context
     * 
     * @return result controlling the workflow
     */
    protected abstract Uni<ExecutionResult> executeSystem(NodeContext context);
}