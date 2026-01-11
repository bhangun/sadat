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
import tech.kayys.wayang.organization.domain.Organization;

@Entity
@Table(name = "marketplace_reviews")
public class MarketplaceReview extends PanacheEntityBase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID reviewId;
    
    @ManyToOne
    @JoinColumn(name = "listing_id")
    public MarketplaceListing listing;
    
    @ManyToOne
    @JoinColumn(name = "organization_id")
    public Organization reviewer;
    
    @Column(name = "rating")
    public int rating; // 1-5 stars
    
    @Column(name = "title")
    public String title;
    
    @Column(name = "comment", columnDefinition = "text")
    public String comment;
    
    @Column(name = "created_at")
    public Instant createdAt;
    
    @Column(name = "helpful_count")
    public int helpfulCount = 0;
    
    @Column(name = "verified_purchase")
    public boolean verifiedPurchase = false;
}
