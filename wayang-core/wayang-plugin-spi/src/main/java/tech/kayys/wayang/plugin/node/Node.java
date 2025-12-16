package tech.kayys.wayang.plugin.node;

import java.util.Optional;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.plugin.ExecutionResult;
import tech.kayys.wayang.plugin.node.exception.NodeException;
import tech.kayys.wayang.plugin.node.exception.NodeExecutionException;
import tech.kayys.wayang.plugin.node.exception.TransientNodeException;

/**
 * Base interface for all executable nodes in the Wayang platform.
 * Follows the triplet pattern: inputs → execute → outputs (success, error)
 * 
 * All implementations must be:
 * - Idempotent (safe to retry)
 * - Thread-safe
 * - Resource-aware (respect quotas)
 * - Observable (emit metrics/traces)
 */
public interface Node {

    /**
     * Lifecycle hook: called once when node is loaded into runtime
     * 
     * @param descriptor The immutable node descriptor
     * @param config     Runtime configuration
     */
    Uni<Void> onLoad(NodeDescriptor descriptor, NodeConfig config) throws NodeException;

    /**
     * Execute the node.
     *
     * @param context the execution context containing inputs, variables,
     *                metadata, and runtime state.
     *
     * @return a Uni wrapping the {@link ExecutionResult}, allowing reactive
     *         composition under Quarkus + Mutiny.
     *
     * @throws NodeExecutionException
     *                                when the node fails permanently and should NOT
     *                                be retried.
     *
     * @throws TransientNodeException
     *                                when the node fails temporarily (API timeout,
     *                                external
     *                                dependency unavailable) and SHOULD be retried
     *                                based on
     *                                workflow retry policy.
     */
    Uni<ExecutionResult> execute(NodeContext context)
            throws NodeExecutionException, TransientNodeException;

    /**
     * Lifecycle hook: called when node is unloaded from runtime
     */
    Uni<Void> onUnload();

    /**
     * Optional: Checkpoint current state for resumability
     * 
     * @param context Current execution context
     * @return Checkpoint state or empty if not supported
     */
    default Optional<CheckpointState> checkpoint(NodeContext context) {
        return Optional.empty();
    }

    /**
     * Resumes this node from a saved checkpoint.
     * Default implementation simply calls {@link #execute(NodeContext)} again.
     *
     * This method MUST be idempotent.
     *
     * @param context    runtime context for this execution attempt.
     * @param checkpoint immutable snapshot of node state saved by the engine.
     *
     * @return Uni producing an {@link ExecutionResult} upon completion.
     *
     * @throws NodeExecutionException if the node fails permanently.
     * @throws TransientNodeException if retry is needed due to a temporary failure.
     */
    default Uni<ExecutionResult> resume(NodeContext context, CheckpointState checkpoint)
            throws NodeExecutionException, TransientNodeException {
        return execute(context);
    }

    /**
     * Health check - verify node can execute
     */
    default Uni<HealthStatus> healthCheck() {
        return Uni.createFrom().item(HealthStatus.healthy());
    }
}
