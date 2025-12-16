package tech.kayys.wayang.domain;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import tech.kayys.wayang.model.LogicDefinition;
import tech.kayys.wayang.model.RuntimeConfig;
import tech.kayys.wayang.model.UIDefinition;
import tech.kayys.wayang.model.ValidationResult;

/**
 * Workflow - Complete workflow definition with logic and UI layers.
 */
@Entity
@Table(name = "workflows", indexes = {
        @Index(name = "idx_workflow_workspace", columnList = "workspace_id"),
        @Index(name = "idx_workflow_tenant", columnList = "tenant_id"),
        @Index(name = "idx_workflow_version", columnList = "version")
})
public class Workflow extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    public Workspace workspace;

    @Column(nullable = false, length = 128)
    public String name;

    @Column(length = 2000)
    public String description;

    @Column(name = "tenant_id", nullable = false)
    public String tenantId;

    @Column(nullable = false, length = 32)
    public String version;

    @Column(name = "created_by", nullable = false)
    public String createdBy;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;

    @Column(name = "published_at")
    public Instant publishedAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    public WorkflowStatus status = WorkflowStatus.DRAFT;

    /**
     * Logic layer - nodes, connections, rules (JSONB)
     */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "logic_definition", columnDefinition = "jsonb", nullable = false)
    public LogicDefinition logic;

    /**
     * UI layer - canvas state, node positions (JSONB)
     */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "ui_definition", columnDefinition = "jsonb")
    public UIDefinition ui;

    /**
     * Runtime configuration (JSONB)
     */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "runtime_config", columnDefinition = "jsonb")
    public RuntimeConfig runtime;

    /**
     * Validation results cache
     */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "validation_result", columnDefinition = "jsonb")
    public ValidationResult validationResult;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    public Map<String, Object> metadata;

    @Version
    public Long entityVersion; // Optimistic locking

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
        if (metadata == null)
            metadata = new HashMap<>();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public enum WorkflowStatus {
        DRAFT, // Being edited
        VALIDATING, // Validation in progress
        VALID, // Passed validation
        INVALID, // Failed validation
        PUBLISHED, // Immutable published version
        ARCHIVED, // Historical version
        DELETED // Soft deleted
    }

    // Domain logic
    public boolean canPublish() {
        return status == WorkflowStatus.VALID && validationResult != null
                && validationResult.isValid();
    }

    public boolean isPublished() {
        return status == WorkflowStatus.PUBLISHED;
    }

    // Multi-tenant queries
    public static Uni<List<Workflow>> findByWorkspace(UUID workspaceId, String tenantId) {
        return find("workspace.id = ?1 and tenantId = ?2 and status != ?3",
                workspaceId, tenantId, WorkflowStatus.DELETED)
                .list();
    }

    public static Uni<Workflow> findLatestVersion(UUID workspaceId, String name, String tenantId) {
        return find("workspace.id = ?1 and name = ?2 and tenantId = ?3 " +
                "and status != ?4 order by createdAt desc",
                workspaceId, name, tenantId, WorkflowStatus.DELETED)
                .firstResult();
    }
}
