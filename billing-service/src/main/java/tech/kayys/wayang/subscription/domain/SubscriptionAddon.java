package tech.kayys.wayang.subscription.domain;

import java.math.BigDecimal;
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
import tech.kayys.wayang.billing.domain.AddonCatalog;

/**
 * Subscription addon
 */
@Entity
@Table(name = "mgmt_subscription_addons")
public class SubscriptionAddon extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "addon_id")
    public UUID addonId;

    @ManyToOne
    @JoinColumn(name = "subscription_id")
    public Subscription subscription;

    @ManyToOne
    @JoinColumn(name = "addon_catalog_id")
    public AddonCatalog addonCatalog;

    @Column(name = "quantity")
    public int quantity = 1;

    @Column(name = "price", precision = 19, scale = 4)
    public BigDecimal price;

    @Column(name = "is_active")
    public boolean isActive = true;

    @Column(name = "added_at")
    public Instant addedAt;

    @Column(name = "expires_at")
    public Instant expiresAt;

    @Column(name = "removed_at")
    public Instant removedAt;
}
