package tech.kayys.wayang.workflow.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Error recovery snapshot
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class ErrorRecoverySnapshot {
    private String failedNodeId;
    private ErrorSnapshot lastError;
    private String recoveryStrategy;
    private Integer attemptsMade;
    private Integer maxAttempts;
    private Instant nextRetryAt;
}
