package tech.kayys.wayang.workflow.kernel;

import java.time.Instant;

/**
 * 🔒 Security token for execution.
 * Enables zero-trust workers and replay protection.
 */
public interface ExecutionToken {

    String getRunId();

    String getNodeExecutionId();

    int getAttempt();

    Instant getIssuedAt();

    Instant getExpiresAt();

    String getSignature();

    boolean isValid();

    boolean isExpired();

    // Factory method for creating derived tokens (for retries)
    ExecutionToken forRetry(int newAttempt);
}
