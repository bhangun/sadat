package tech.kayys.wayang.invoice.domain;

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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import tech.kayys.wayang.billing.model.LineItemType;

@Entity
@Table(name = "mgmt_invoice_line_items")
public class InvoiceLineItem extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "line_item_id")
    public UUID lineItemId;

    @ManyToOne
    @JoinColumn(name = "invoice_id")
    public Invoice invoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type")
    public LineItemType itemType;

    @Column(name = "description")
    public String description;

    @Column(name = "quantity", precision = 19, scale = 4)
    public BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_price", precision = 19, scale = 4)
    public BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "amount", precision = 19, scale = 4)
    public BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "period_start")
    public Instant periodStart;

    @Column(name = "period_end")
    public Instant periodEnd;

    @Column(name = "taxable")
    public boolean taxable = true;

    @Column(name = "tax_rate", precision = 5, scale = 4)
    public BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "subscription_id")
    public UUID subscriptionId;

    @Column(name = "usage_aggregate_id")
    public UUID usageAggregateId;

    @Column(name = "metadata", columnDefinition = "jsonb")
    public Map<String, Object> metadata = new HashMap<>();
}
