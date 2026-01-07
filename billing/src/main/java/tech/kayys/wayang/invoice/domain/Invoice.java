package tech.kayys.wayang.invoice.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import tech.kayys.wayang.invoice.model.InvoiceStatus;
import tech.kayys.wayang.organization.domain.Organization;
import tech.kayys.wayang.subscription.domain.Subscription;

@Entity
@Table(name = "mgmt_invoices", indexes = {
        @Index(name = "idx_invoice_org", columnList = "organization_id"),
        @Index(name = "idx_invoice_number", columnList = "invoice_number", unique = true),
        @Index(name = "idx_invoice_status", columnList = "status"),
        @Index(name = "idx_invoice_due_date", columnList = "due_date")
})
public class Invoice extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "invoice_id")
    public UUID invoiceId;

    @Column(name = "invoice_number", unique = true, length = 50)
    public String invoiceNumber;

    @ManyToOne
    @JoinColumn(name = "organization_id")
    public Organization organization;

    @ManyToOne
    @JoinColumn(name = "subscription_id")
    public Subscription subscription;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    public InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "period_start")
    public Instant periodStart;

    @Column(name = "period_end")
    public Instant periodEnd;

    @Column(name = "invoice_date")
    public Instant invoiceDate;

    @Column(name = "due_date")
    public Instant dueDate;

    @Column(name = "paid_at")
    public Instant paidAt;

    @Column(name = "subtotal", precision = 19, scale = 4)
    public BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 19, scale = 4)
    public BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "tax_rate", precision = 5, scale = 4)
    public BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 19, scale = 4)
    public BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 19, scale = 4)
    public BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "amount_paid", precision = 19, scale = 4)
    public BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "amount_due", precision = 19, scale = 4)
    public BigDecimal amountDue = BigDecimal.ZERO;

    @Column(name = "currency", length = 3)
    public String currency = "USD";

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<InvoiceLineItem> lineItems = new ArrayList<>();

    @Column(name = "payment_method_id")
    public String paymentMethodId;

    @Column(name = "payment_intent_id")
    public String paymentIntentId;

    @Column(name = "payment_transaction_id")
    public String paymentTransactionId;

    @Column(name = "external_invoice_id")
    public String externalInvoiceId;

    @Column(name = "pdf_url")
    public String pdfUrl;

    @Column(name = "attempt_count")
    public int attemptCount = 0;

    @Column(name = "last_attempt_at")
    public Instant lastAttemptAt;

    @Column(name = "next_attempt_at")
    public Instant nextAttemptAt;

    @Column(name = "notes", columnDefinition = "text")
    public String notes;

    @Column(name = "metadata", columnDefinition = "jsonb")
    public Map<String, Object> metadata = new HashMap<>();

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;

    @Column(name = "finalized_at")
    public Instant finalizedAt;

    // Business methods

    public void addLineItem(InvoiceLineItem item) {
        item.invoice = this;
        lineItems.add(item);
        recalculateTotals();
    }

    public void recalculateTotals() {
        this.subtotal = lineItems.stream()
                .map(item -> item.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalAmount = subtotal.subtract(discountAmount).add(taxAmount);
        this.amountDue = totalAmount.subtract(amountPaid);
        this.updatedAt = Instant.now();
    }

    public void applyTax(BigDecimal rate) {
        this.taxRate = rate;
        this.taxAmount = subtotal.multiply(rate).divide(
                BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        recalculateTotals();
    }

    public void finalize() {
        if (status != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only draft invoices can be finalized");
        }
        this.status = InvoiceStatus.OPEN;
        this.finalizedAt = Instant.now();
        this.invoiceDate = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markPaid(String transactionId, BigDecimal amount) {
        this.amountPaid = this.amountPaid.add(amount);
        this.paymentTransactionId = transactionId;
        this.paidAt = Instant.now();

        if (amountPaid.compareTo(totalAmount) >= 0) {
            this.status = InvoiceStatus.PAID;
            this.amountDue = BigDecimal.ZERO;
        } else {
            this.status = InvoiceStatus.PARTIALLY_PAID;
            this.amountDue = totalAmount.subtract(amountPaid);
        }

        this.updatedAt = Instant.now();
    }

    public void markVoid(String reason) {
        this.status = InvoiceStatus.VOID;
        this.metadata.put("void_reason", reason);
        this.updatedAt = Instant.now();
    }

    public boolean isOverdue() {
        return status == InvoiceStatus.OPEN &&
                dueDate != null &&
                Instant.now().isAfter(dueDate);
    }

    public long getDaysOverdue() {
        if (!isOverdue())
            return 0;
        return ChronoUnit.DAYS.between(dueDate, Instant.now());
    }
}