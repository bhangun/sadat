package tech.kayys.wayang.marketplace.domain;

import java.time.Instant;
import java.util.UUID;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import tech.kayys.silat.model.WorkflowDefinition;

/**
 * Listing version (for updates)
 */
@Entity
@Table(name = "marketplace_listing_versions")
class ListingVersion extends PanacheEntityBase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID versionId;
    
    @ManyToOne
    @JoinColumn(name = "listing_id")
    public MarketplaceListing listing;
    
    @Column(name = "version")
    public String version;
    
    @Column(name = "release_notes", columnDefinition = "text")
    public String releaseNotes;
    
    @Column(name = "workflow_template", columnDefinition = "jsonb")
    public WorkflowDefinition workflowTemplate;
    
    @Column(name = "released_at")
    public Instant releasedAt;
    
    @Column(name = "install_count")
    public long installCount = 0;
}

