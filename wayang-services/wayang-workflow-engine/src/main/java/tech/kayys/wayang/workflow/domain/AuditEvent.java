package tech.kayys.wayang.workflow.domain;

import java.time.Instant;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import tech.kayys.wayang.workflow.model.ActorType;
import tech.kayys.wayang.workflow.model.EventType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Audit Event Entity.
 */
@Entity
@Table(name = "audit_events")
@lombok.Data
@lombok.NoArgsConstructor
public class AuditEvent {

    @Id
    private String id;

    @Column(nullable = false)
    private String runId;

    private String nodeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> eventData;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActorType actorType;

    private String actorId;

    @Column(nullable = false)
    private Instant timestamp;

    private String hash;

    private String summary;
}