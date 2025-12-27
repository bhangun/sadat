package tech.kayys.wayang.workflow.saga.model;

import java.time.Instant;

/**
 * Saga Execution Record
 */
@lombok.Data
@lombok.NoArgsConstructor
public class SagaExecution {
    String id;
    String runId;
    String sagaDefId;
    CompensationStrategy strategy;
    SagaStatus status;
    Instant startedAt;
    Instant completedAt;
    String errorMessage;

    public SagaExecution(
            String id,
            String runId,
            String sagaDefId,
            CompensationStrategy strategy,
            SagaStatus status,
            Instant startedAt) {
        this.id = id;
        this.runId = runId;
        this.sagaDefId = sagaDefId;
        this.strategy = strategy;
        this.status = status;
        this.startedAt = startedAt;
    }

    public void complete() {
        this.status = SagaStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void fail(String errorMessage) {
        this.status = SagaStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
    }
}
