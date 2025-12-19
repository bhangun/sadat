package tech.kayys.wayang.common.spi;

import io.smallrye.mutiny.Uni;
/**
 * IntegrationNode: For deterministic I/O operations
 * Examples: HTTP calls, database queries, message queue operations
 */
public abstract class IntegrationNode extends AbstractNode {
    
    @Override
    protected final Uni<ExecutionResult> doExecute(NodeContext context) {
        return executeIntegration(context);
    }

    /**
     * Implement integration logic here.
     * Should be deterministic and side-effect explicit.
     */
    protected abstract Uni<ExecutionResult> executeIntegration(NodeContext context);
}
