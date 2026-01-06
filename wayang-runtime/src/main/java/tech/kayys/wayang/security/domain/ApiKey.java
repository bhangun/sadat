package tech.kayys.wayang.security.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * API Key entity for frontend authentication
 */
@Entity
@Table(name = "api_keys")
public class ApiKey extends io.quarkus.hibernate.reactive.panache.PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "tenant_id", nullable = false)
    public String tenantId;

    @Column(name = "key_hash", nullable = false, unique = true)
    public String keyHash;

    @Column(name = "key_prefix", nullable = false)
    public String keyPrefix; // First 8 chars for identification

    @Column(name = "name", nullable = false)
    public String name;

    @Column(name = "description")
    public String description;

    @Column(name = "created_by", nullable = false)
    public String createdBy;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "expires_at")
    public Instant expiresAt;

    @Column(name = "last_used_at")
    public Instant lastUsedAt;

    @Column(name = "is_active")
    public boolean isActive = true;

    @Column(name = "scopes", columnDefinition = "jsonb")
    public List<String> scopes; // Permissions for this key

    @Column(name = "rate_limit")
    public Integer rateLimit; // Requests per minute

    @Column(name = "metadata", columnDefinition = "jsonb")
    public Map<String, Object> metadata;
}
