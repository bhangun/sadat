package tech.kayys.wayang.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import tech.kayys.wayang.model.LogicDefinition;
import tech.kayys.wayang.model.UIDefinition;

/**
 * WorkflowDraft - Auto-saved draft snapshots
 */
@Entity
@Table(name = "workflow_drafts", indexes = {
        @Index(name = "idx_draft_workflow", columnList = "workflow_id"),
        @Index(name = "idx_draft_user", columnList = "user_id")
})
public class WorkflowDraft extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @Column(name = "workflow_id", nullable = false)
    public UUID workflowId;

    @Column(name = "user_id", nullable = false)
    public String userId;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "draft_content", columnDefinition = "jsonb", nullable = false)
    public WorkflowSnapshot content;

    @Column(name = "saved_at", nullable = false)
    public Instant savedAt;

    @Column(name = "auto_saved")
    public boolean autoSaved = true;

    @PrePersist
    void prePersist() {
        savedAt = Instant.now();
    }

    public static class WorkflowSnapshot {
        public LogicDefinition logic;
        public UIDefinition ui;
        public Map<String, Object> metadata;
    }
}