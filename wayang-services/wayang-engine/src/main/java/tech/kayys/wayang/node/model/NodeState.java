package tech.kayys.wayang.node.model;

/**
 * Node execution state enum.
 */
enum NodeState {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    SKIPPED,
    CANCELLED,
    AWAITING_RETRY,
    AWAITING_HITL;

    static NodeState fromResult(NodeExecutionResult result) {
        if (result.isSuccess())
            return COMPLETED;
        if (result.isError())
            return FAILED;
        if (result.isCancelled())
            return CANCELLED;
        if (result.isAwaitingHuman())
            return AWAITING_HITL;
        return PENDING;
    }
}
