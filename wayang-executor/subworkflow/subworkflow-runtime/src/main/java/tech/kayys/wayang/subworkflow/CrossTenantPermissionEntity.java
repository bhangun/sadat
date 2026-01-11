package tech.kayys.silat.persistence.subworkflow;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Cross-tenant permission entity
 */
@Entity
@Table(
    name = "cross_tenant_permissions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_tenant_pair",
            columnNames = {"source_tenant_id", "target_tenant_id"}
        )
    }
)
public class CrossTenantPermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "permission_id")
    private UUID permissionId;

    @Column(name = "source_tenant_id", nullable = false)
    private String sourceTenantId;

    @Column(name = "target_tenant_id", nullable = false)
    private String targetTenantId;

    @Column(name = "permissions", columnDefinition = "text[]")
    private String[] permissions;

    @Column(name = "granted_at")
    private Instant grantedAt;

    @Column(name = "granted_by")
    private String grantedBy;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // Getters and setters
    public UUID getPermissionId() { return permissionId; }
    public String getSourceTenantId() { return sourceTenantId; }
    public void setSourceTenantId(String sourceTenantId) {
        this.sourceTenantId = sourceTenantId;
    }

    public String getTargetTenantId() { return targetTenantId; }
    public void setTargetTenantId(String targetTenantId) {
        this.targetTenantId = targetTenantId;
    }

    public String[] getPermissions() { return permissions; }
    public void setPermissions(String[] permissions) {
        this.permissions = permissions;
    }

    public Instant getGrantedAt() { return grantedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}