package tech.kayys.wayang;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "api_keys")
public class ApiKey extends PanacheEntity {

    @Column(nullable = false, unique = true)
    public String keyHash;

    public String keyPrefix;
    public String name;
    public String description;
    
    public Instant createdAt;
    public Instant expiresAt;
    public Instant lastUsedAt;

    public String consumerId;
    public String tenantId;
    public String workspaceId;
    public String planId;

    @ElementCollection
    @CollectionTable(name = "api_key_scopes", joinColumns = @JoinColumn(name = "api_key_id"))
    @Column(name = "scope")
    public Set<String> scopes;

    @ElementCollection
    @CollectionTable(name = "api_key_metadata", joinColumns = @JoinColumn(name = "api_key_entity_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    public Map<String, String> metadata;

    public boolean active;
}
