package tech.kayys.wayang.billing.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import tech.kayys.wayang.billing.model.QuotaStatus;
import tech.kayys.wayang.billing.model.UsageType;
import tech.kayys.wayang.organization.domain.Organization;

/**
 * Usage aggregate for billing period
 */
@Entity
@Table(name = "mgmt_usage_aggregates", indexes = {
    @Index(name = "idx_agg_org_period", columnList = "organization_id, year_month", unique = true)
})
public class UsageAggregate extends PanacheEntityBase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "aggregate_id")
    public UUID aggregateId;
    
    @ManyToOne
    @JoinColumn(name = "organization_id")
    public Organization organization;
    
    /**
     * Billing period (YYYY-MM)
     */
    @Column(name = "year_month")
    public YearMonth yearMonth;
    
    /**
     * Aggregated usage by type
     */
    @Column(name = "usage_by_type", columnDefinition = "jsonb")
    public Map<UsageType, Long> usageByType = new HashMap<>();
    
    /**
     * Total cost for period
     */
    @Column(name = "total_cost", precision = 19, scale = 4)
    public BigDecimal totalCost = BigDecimal.ZERO;
    
    /**
     * Cost breakdown
     */
    @Column(name = "cost_breakdown", columnDefinition = "jsonb")
    public Map<String, BigDecimal> costBreakdown = new HashMap<>();
    
    /**
     * Quota status
     */
    @Column(name = "quota_status", columnDefinition = "jsonb")
    public Map<String, QuotaStatus> quotaStatus = new HashMap<>();
    
    @Column(name = "computed_at")
    public Instant computedAt;
    
    @Column(name = "finalized")
    public boolean finalized = false;
}
