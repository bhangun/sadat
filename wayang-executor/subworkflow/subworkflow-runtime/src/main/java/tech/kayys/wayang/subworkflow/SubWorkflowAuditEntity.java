package tech.kayys.silat.persistence.subworkflow;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Sub-workflow audit entity
 */
@Entity
@Table(
    name = "sub_workflow_audit",
    indexes = {
        @Index(name = "idx_audit_run", columnList = "run_id"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp")
    }
)
public class SubWorkflowAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "audit_id")
    private UUID auditId;

    @Column(name = "run_id", nullable = false)
    private String runId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditEventType eventType;

    @Column(name = "event_data", columnDefinition = "jsonb")
    private String eventData;

    @Column(name = "timestamp")
    private Instant timestamp;

    @Column(name = "user_id")
    private String userId;

    // Getters and setters
    public UUID getAuditId() { return auditId; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public AuditEventType getEventType() { return eventType; }
    public void setEventType(AuditEventType eventType) {
        this.eventType = eventType;
    }

    public String getEventData() { return eventData; }
    public void setEventData(String eventData) {
        this.eventData = eventData;
    }

    public Instant getTimestamp() { return timestamp; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}