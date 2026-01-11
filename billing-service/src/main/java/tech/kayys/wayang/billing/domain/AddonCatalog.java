package tech.kayys.wayang.billing.domain;

import java.math.BigDecimal;
import java.util.HashMap;
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
import jakarta.persistence.Table;
import tech.kayys.wayang.billing.model.AddonType;

/**
 * Addon catalog - Available add-ons
 */
@Entity
@Table(name = "mgmt_addon_catalog")
public class AddonCatalog extends PanacheEntityBase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "catalog_id")
    public UUID catalogId;
    
    @Column(name = "addon_code", unique = true)
    public String addonCode;
    
    @Column(name = "name")
    public String name;
    
    @Column(name = "description", columnDefinition = "text")
    public String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "addon_type")
    public AddonType addonType;
    
    @Column(name = "unit_price", precision = 19, scale = 4)
    public BigDecimal unitPrice;
    
    @Column(name = "billing_unit")
    public String billingUnit; // e.g., "per 1000 runs", "per user"
    
    @Column(name = "resource_increment", columnDefinition = "jsonb")
    public Map<String, Integer> resourceIncrement = new HashMap<>();
    
    @Column(name = "is_active")
    public boolean isActive = true;
}