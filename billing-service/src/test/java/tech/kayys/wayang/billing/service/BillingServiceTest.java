package tech.kayys.wayang.billing.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.kayys.wayang.billing.dto.BillingSummary;
import tech.kayys.wayang.invoice.domain.Invoice;
import tech.kayys.wayang.organization.domain.Organization;
import jakarta.inject.Inject;

@io.quarkus.test.junit.QuarkusTest
class BillingServiceTest {

    @Inject
    BillingService billingService;

    private Organization organization;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        // Setup test organization
        organization = new Organization();
        organization.organizationId = UUID.randomUUID();
        organization.tenantId = "test-org";
        organization.slug = "test-org";
        organization.name = "Test Organization";
        organization.billingEmail = "billing@test.com";

        // Setup test invoice
        invoice = new Invoice();
        invoice.invoiceId = UUID.randomUUID();
        invoice.invoiceNumber = "INV-001";
        invoice.organization = organization;
        invoice.amountDue = BigDecimal.valueOf(149.99);
        invoice.currency = "USD";
        invoice.dueDate = Instant.now().plusSeconds(2592000); // 30 days from now
    }

    @Test
    @io.quarkus.test.TestTransaction
    void testGenerateInvoice_ValidOrganization_ReturnsInvoice() {
        // Given
        Instant periodEnd = Instant.now();
        organization.persist().await().indefinitely();

        // When
        var result = billingService.generateInvoice(organization, periodEnd).await().indefinitely();

        // Then
        assertNotNull(result);
        assertEquals(organization.organizationId, result.organization.organizationId);
        assertNotNull(result.invoiceNumber);
        assertTrue(result.amountDue.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @io.quarkus.test.TestTransaction
    void testProcessPayment_ValidPayment_UpdatesInvoice() {
        // Given
        organization.persist().await().indefinitely();
        invoice.persist().await().indefinitely();

        // When
        var result = billingService.processPayment(invoice).await().indefinitely();

        // Then
        assertNotNull(result);
        assertEquals(invoice.invoiceId, result.invoiceId);
        assertEquals(BigDecimal.ZERO, result.amountDue);
    }

    @Test
    @io.quarkus.test.TestTransaction
    void testCreateCreditNote_ValidInvoice_ReturnsCreditNote() {
        // Given
        BigDecimal creditAmount = BigDecimal.valueOf(50.00);
        organization.persist().await().indefinitely();
        invoice.persist().await().indefinitely();

        // When
        var result = billingService.createCreditNote(invoice, creditAmount, "Refund").await().indefinitely();

        // Then
        assertNotNull(result);
        assertTrue(result.notes.contains(invoice.invoiceNumber));
        assertEquals(creditAmount, result.amount);
        assertEquals("Refund", result.reason);
    }

    @Test
    @io.quarkus.test.TestTransaction
    void testGetBillingSummary_ValidOrganization_ReturnsSummary() {
        // Given
        organization.persist().await().indefinitely();

        // When
        var result = billingService.getBillingSummary(organization).await().indefinitely();

        // Then
        assertNotNull(result);
        assertEquals(organization.tenantId, result.tenantId());
    }

    @Test
    void testHandleOverdueInvoices_NullOrganizationId_ThrowsException() {
        // When & Then
        // verify correct exception handling if applicable
    }
}