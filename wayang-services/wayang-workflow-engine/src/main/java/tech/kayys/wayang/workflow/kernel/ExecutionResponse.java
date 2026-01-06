package tech.kayys.wayang.workflow.kernel;

import java.time.Instant;

/**
 * 🔒 Execution response from external executors
 */
public interface ExecutionResponse {

    String getRequestId();

    String getExecutorId();

    Instant getReceivedAt();

    // Either result or error
    NodeExecutionResult getResult();

    ExecutorError getError();

    /**
     * Validate signature
     */
    boolean isValid(String sharedSecret);
}
