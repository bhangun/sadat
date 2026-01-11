package tech.kayys.wayang.invoice.model;

public enum InvoiceStatus {
    DRAFT,
    OPEN,
    PAID,
    PARTIALLY_PAID,
    VOID,
    UNCOLLECTIBLE,
    REFUNDED
}