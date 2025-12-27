package tech.kayys.wayang.workflow.domain;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import tech.kayys.wayang.workflow.saga.model.CompensationStrategy;
import tech.kayys.wayang.workflow.saga.model.SagaStatus;

@Entity
@Table(name = "saga_executions")
@lombok.Data
@lombok.NoArgsConstructor
@lombok.EqualsAndHashCode(callSuper = false)
public class SagaExecutionEntity {
    @Id
    private String id;
    private String runId;
    private String sagaDefId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private CompensationStrategy strategy;
    @Enumerated(EnumType.STRING)
    private SagaStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private String errorMessage;

}
