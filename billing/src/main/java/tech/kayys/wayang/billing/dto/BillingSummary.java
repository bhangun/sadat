package tech.kayys.wayang.billing.dto;

import java.math.BigDecimal;
import java.util.List;

import tech.kayys.wayang.invoice.domain.Invoice;

public record BillingSummary(
        String tenantId,
        BigDecimal outstandingBalance,
        BigDecimal currentPeriodUsage,
        BigDecimal upcomingInvoiceEstimate,
        List<Invoice> paymentHistory) {
}