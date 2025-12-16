package tech.kayys.wayang.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.*;

/**
 * Workspace - Top-level organizational container for workflows.
 * Supports multi-tenancy isolation.
 */
@Entity
@Table(name = "workspaces", indexes = {
        @Index(name = "idx_workspace_tenant", columnList = "tenant_id"),
        @Index(name = "idx_workspace_owner", columnList = "owner_id")
})
public class Workspace extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @Column(nullable = false, length = 128)
    public String name;

    @Column(length = 2000)
    public String description;

    @Column(name = "tenant_id", nullable = false)
    public String tenantId;

    @Column(name = "owner_id", nullable = false)
    public String ownerId;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    public Map<String, Object> metadata;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    public WorkspaceStatus status = WorkspaceStatus.ACTIVE;

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

    public enum WorkspaceStatus {
        ACTIVE, ARCHIVED, DELETED
    }

    // Multi-tenant query helpers
    public static Uni<List<Workspace>> findByTenant(String tenantId) {
        return find("tenantId = ?1 and status = ?2", tenantId, WorkspaceStatus.ACTIVE)
                .list();
    }
}
