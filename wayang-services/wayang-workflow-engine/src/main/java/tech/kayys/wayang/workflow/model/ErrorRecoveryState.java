package tech.kayys.wayang.workflow.model;

import java.time.Instant;

import lombok.Data;
import lombok.NoArgsConstructor;
import tech.kayys.wayang.schema.execution.ErrorPayload;

/**
 * Error recovery state
 */
@Data
@NoArgsConstructor
public class ErrorRecoveryState {
    private String failedNodeId;
    private ErrorPayload lastError;
    private String recoveryStrategy; // RETRY, SKIP, COMPENSATE, ESCALATE
    private Integer attemptsMade = 0;
    private Integer maxAttempts = 3;
    private Instant nextRetryAt;
}
