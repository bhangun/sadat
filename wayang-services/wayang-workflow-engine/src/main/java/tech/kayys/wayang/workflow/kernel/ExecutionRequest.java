package tech.kayys.wayang.workflow.kernel;

import java.time.Instant;

/**
 * 🔒 Execution request sent to external executors
 */
public interface ExecutionRequest {

    String getRequestId();

    WorkflowRunId getRunId();

    String getNodeId();

    NodeDescriptor getNode();

    ExecutionContext getContext();

    ExecutionToken getToken();

    Instant getDeadline(); // When executor should respond by

    // Target executor information
    String getTargetExecutorId();

    String getCallbackUrl(); // Where to send completion

    /**
     * Serialize for message bus
     */
    String toMessage();
}