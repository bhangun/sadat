package tech.kayys.wayang.marketplace.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import tech.kayys.wayang.marketplace.model.InstallationStatus;
import tech.kayys.wayang.organization.domain.Organization;

@Entity
@Table(name = "marketplace_installations")
public class MarketplaceInstallation extends PanacheEntityBase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID installationId;
    
    @ManyToOne
    @JoinColumn(name = "listing_id")
    public MarketplaceListing listing;
    
    @ManyToOne
    @JoinColumn(name = "organization_id")
    public Organization organization;
    
    @Column(name = "installed_version")
    public String installedVersion;
    
    @Column(name = "workflow_instance_id")
    public String workflowInstanceId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    public InstallationStatus status;
    
    @Column(name = "installed_at")
    public Instant installedAt;
    
    @Column(name = "last_updated")
    public Instant lastUpdated;
    
    @Column(name = "auto_update")
    public boolean autoUpdate = true;
    
    @Column(name = "configuration", columnDefinition = "jsonb")
    public Map<String, Object> configuration;
}
