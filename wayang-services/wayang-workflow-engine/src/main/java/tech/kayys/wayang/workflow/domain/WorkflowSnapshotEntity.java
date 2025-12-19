package tech.kayys.wayang.workflow.domain;

import java.time.Instant;

import org.hibernate.annotations.Type;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Snapshot JPA Entity
 */
@Entity
@Table(name = "workflow_snapshots", indexes = {
        @Index(name = "idx_snapshots_run", columnList = "run_id, event_count")
})
public class WorkflowSnapshotEntity {
    @Id
    String id;

    @Column(name = "run_id", nullable = false)
    String runId;

    @Column(name = "event_count", nullable = false)
    Long eventCount;

    @Type(io.hypersistence.utils.hibernate.type.json.JsonBinaryType.class)
    @Column(name = "snapshot_data", columnDefinition = "jsonb", nullable = false)
    WorkflowRun snapshotData;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public Long getEventCount() {
        return eventCount;
    }

    public void setEventCount(Long eventCount) {
        this.eventCount = eventCount;
    }

    public WorkflowRun getSnapshotData() {
        return snapshotData;
    }

    public void setSnapshotData(WorkflowRun snapshotData) {
        this.snapshotData = snapshotData;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

}