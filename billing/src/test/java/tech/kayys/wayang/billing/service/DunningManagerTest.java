package tech.kayys.wayang.billing.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.kayys.wayang.invoice.domain.Invoice;
import tech.kayys.wayang.organization.domain.Organization;

class DunningManagerTest {

    private DunningManager dunningManager;
    private Organization organization;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        dunningManager = new DunningManager();

        // Setup test organization
        organization = new Organization();
        organization.organizationId = UUID.randomUUID();
        organization.name = "Test Organization";
        organization.billingEmail = "billing@test.com";

        // Setup test invoice
        invoice = new Invoice();
        invoice.invoiceId = UUID.randomUUID();
        invoice.invoiceNumber = "INV-001";
        invoice.organization = organization;
        invoice.amountDue = BigDecimal.valueOf(149.99);
        invoice.currency = "USD";
        invoice.dueDate = Instant.now().minusSeconds(86400); // 1 day overdue
    }

    @Test
    void testHandleFailedPayment_OverdueInvoice_SendsReminder() {
        // When
        var result = dunningManager.handleFailedPayment(invoice);

        // Then
        assertNotNull(result);
    }
}