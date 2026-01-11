package tech.kayys.wayang.billing.domain;

import java.math.BigDecimal;
import java.time.Instant;
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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import tech.kayys.wayang.billing.model.UsageType;
import tech.kayys.wayang.organization.domain.Organization;

/**
 * Usage Record - Tracks resource consumption
 */
@Entity
@Table(name = "mgmt_usage_records", indexes = {
    @Index(name = "idx_usage_org_period", columnList = "organization_id, period_start, period_end"),
    @Index(name = "idx_usage_type", columnList = "usage_type"),
    @Index(name = "idx_usage_timestamp", columnList = "timestamp")
})
public class UsageRecord extends PanacheEntityBase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "usage_id")
    public UUID usageId;
    
    @ManyToOne
    @JoinColumn(name = "organization_id")
    public Organization organization;
    
    /**
     * Usage type/metric
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type")
    public UsageType usageType;
    
    /**
     * Resource identifier
     */
    @Column(name = "resource_id")
    public String resourceId;
    
    /**
     * Usage quantity
     */
    @Column(name = "quantity")
    public long quantity;
    
    /**
     * Unit of measurement
     */
    @Column(name = "unit")
    public String unit;
    
    /**
     * Timestamp of usage
     */
    @Column(name = "timestamp")
    public Instant timestamp;
    
    /**
     * Billing period this belongs to
     */
    @Column(name = "period_start")
    public Instant periodStart;
    
    @Column(name = "period_end")
    public Instant periodEnd;
    
    /**
     * Cost calculation
     */
    @Column(name = "unit_price", precision = 19, scale = 4)
    public BigDecimal unitPrice;
    
    @Column(name = "total_cost", precision = 19, scale = 4)
    public BigDecimal totalCost;
    
    /**
     * Additional context
     */
    @Column(name = "metadata", columnDefinition = "jsonb")
    public Map<String, Object> metadata = new HashMap<>();
    
    /**
     * Processed flag for billing
     */
    @Column(name = "is_billed")
    public boolean isBilled = false;
    
    @Column(name = "invoice_id")
    public UUID invoiceId;
}
