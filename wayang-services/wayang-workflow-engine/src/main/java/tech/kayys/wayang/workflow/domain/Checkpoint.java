package tech.kayys.wayang.workflow.domain;

import java.time.Instant;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import tech.kayys.wayang.workflow.model.WorkflowExecutionState;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Checkpoint entity
 */
@Entity
@Table(name = "workflow_checkpoints", indexes = {
        @Index(name = "idx_checkpoints_run_id", columnList = "runId,sequenceNumber"),
        @Index(name = "idx_checkpoints_created", columnList = "createdAt")
})
@lombok.Data
@lombok.NoArgsConstructor
@lombok.EqualsAndHashCode(callSuper = false)
public class Checkpoint extends PanacheEntityBase {

    @Id
    @Column(nullable = false, length = 36)
    private String checkpointId;

    @Column(nullable = false, length = 36)
    private String runId;

    @Column(nullable = false, length = 100)
    private String tenantId;

    @Column(nullable = false)
    private Integer sequenceNumber;

    @Column(columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private WorkflowExecutionState executionState;

    @Column(length = 30)
    private String status;

    @Column(length = 30)
    private String phase;

    @Column(nullable = false)
    private Integer nodesExecuted;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Long sizeBytes;
}
