package tech.kayys.wayang.workflow.domain;

import java.time.Instant;
import java.util.Map;

import org.hibernate.annotations.Type;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import tech.kayys.wayang.workflow.api.model.WorkflowEventType;

/**
 * Workflow Event Entity (JPA)
 */
@Entity
@Table(name = "workflow_events", indexes = {
        @Index(name = "idx_events_run_seq", columnList = "run_id, sequence"),
        @Index(name = "idx_events_created", columnList = "created_at")
})
public class WorkflowEventEntity {

    @Id
    String id;

    @Column(name = "run_id", nullable = false)
    String runId;

    @Column(nullable = false)
    Long sequence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    WorkflowEventType type;

    @Type(io.hypersistence.utils.hibernate.type.json.JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    Map<String, Object> data;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Version
    Long version;

    public WorkflowEventEntity() {
    }

    public WorkflowEventEntity(
            String id,
            String runId,
            Long sequence,
            WorkflowEventType type,
            Map<String, Object> data,
            Instant createdAt) {
        this.id = id;
        this.runId = runId;
        this.sequence = sequence;
        this.type = type;
        this.data = data;
        this.createdAt = createdAt;
    }
}
