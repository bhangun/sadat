package tech.kayys.wayang;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.util.Set;

@Entity
@Table(name = "api_keys")
public class ApiKey extends PanacheEntity {

    @Column(nullable = false, unique = true)
    public String keyHash;

    public String consumerId;
    public String tenantId;
    public String workspaceId;
    public String planId;

    @ElementCollection
    @CollectionTable(name = "api_key_scopes", joinColumns = @JoinColumn(name = "api_key_id"))
    @Column(name = "scope")
    public Set<String> scopes;

    public boolean active;
}
