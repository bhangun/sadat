package tech.kayys.wayang.billing.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.billing.domain.Address;
import tech.kayys.wayang.invoice.domain.Invoice;
import tech.kayys.wayang.organization.domain.Organization;

class TaxCalculatorTest {

    private TaxCalculator taxCalculator;

    private Organization organization;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        taxCalculator = new TaxCalculator();

        // Setup test organization
        organization = new Organization();
        organization.organizationId = UUID.randomUUID();
        organization.name = "Test Organization";

        // Setup test invoice
        invoice = new Invoice();
        invoice.organization = organization;
        invoice.subtotal = BigDecimal.valueOf(100.00); // $100 before tax
    }

    @Test
    void testCalculateTax_US_TaxRate() {
        // Given
        Address address = new Address();
        address.country = "US";
        organization.billingAddress = address;

        // When
        Uni<BigDecimal> result = taxCalculator.calculateTax(invoice);

        // Then
        BigDecimal taxAmount = result.await().indefinitely();
        assertEquals(BigDecimal.valueOf(8.00), taxAmount); // 8% of $100
    }

    @Test
    void testCalculateTax_Canada_TaxRate() {
        // Given
        Address address = new Address();
        address.country = "CA";
        organization.billingAddress = address;

        // When
        Uni<BigDecimal> result = taxCalculator.calculateTax(invoice);

        // Then
        BigDecimal taxAmount = result.await().indefinitely();
        assertEquals(BigDecimal.valueOf(13.00), taxAmount); // 13% of $100
    }

    @Test
    void testCalculateTax_UK_TaxRate() {
        // Given
        Address address = new Address();
        address.country = "GB";
        organization.billingAddress = address;

        // When
        Uni<BigDecimal> result = taxCalculator.calculateTax(invoice);

        // Then
        BigDecimal taxAmount = result.await().indefinitely();
        assertEquals(BigDecimal.valueOf(20.00), taxAmount); // 20% of $100
    }

    @Test
    void testCalculateTax_Germany_TaxRate() {
        // Given
        Address address = new Address();
        address.country = "DE";
        organization.billingAddress = address;

        // When
        Uni<BigDecimal> result = taxCalculator.calculateTax(invoice);

        // Then
        BigDecimal taxAmount = result.await().indefinitely();
        assertEquals(BigDecimal.valueOf(19.00), taxAmount); // 19% of $100
    }

    @Test
    void testCalculateTax_UnknownCountry_NoTax() {
        // Given
        Address address = new Address();
        address.country = "FR"; // France - not in our tax rate table
        organization.billingAddress = address;

        // When
        Uni<BigDecimal> result = taxCalculator.calculateTax(invoice);

        // Then
        BigDecimal taxAmount = result.await().indefinitely();
        assertEquals(BigDecimal.ZERO, taxAmount); // No tax for unknown countries
    }

    @Test
    void testCalculateTax_NoBillingAddress_DefaultsToUS() {
        // Given
        organization.billingAddress = null; // No billing address

        // When
        Uni<BigDecimal> result = taxCalculator.calculateTax(invoice);

        // Then
        BigDecimal taxAmount = result.await().indefinitely();
        assertEquals(BigDecimal.valueOf(8.00), taxAmount); // Defaults to US tax rate
    }

    @Test
    void testCalculateTax_CaseInsensitiveCountry() {
        // Given
        Address address = new Address();
        address.country = "us"; // lowercase
        organization.billingAddress = address;

        // When
        Uni<BigDecimal> result = taxCalculator.calculateTax(invoice);

        // Then
        BigDecimal taxAmount = result.await().indefinitely();
        assertEquals(BigDecimal.valueOf(8.00), taxAmount); // Should still work with lowercase
    }

    @Test
    void testCalculateTax_DifferentSubtotalAmounts() {
        // Given
        Address address = new Address();
        address.country = "US";
        organization.billingAddress = address;

        Invoice testInvoice = new Invoice();
        testInvoice.organization = organization;
        testInvoice.subtotal = BigDecimal.valueOf(250.75); // Different amount

        // When
        Uni<BigDecimal> result = taxCalculator.calculateTax(testInvoice);

        // Then
        BigDecimal taxAmount = result.await().indefinitely();
        assertEquals(BigDecimal.valueOf(20.06), taxAmount); // 8% of $250.75, rounded to 2 decimals
    }

    @Test
    void testCalculateTax_ZeroSubtotal() {
        // Given
        Address address = new Address();
        address.country = "US";
        organization.billingAddress = address;

        Invoice testInvoice = new Invoice();
        testInvoice.organization = organization;
        testInvoice.subtotal = BigDecimal.ZERO;

        // When
        Uni<BigDecimal> result = taxCalculator.calculateTax(testInvoice);

        // Then
        BigDecimal taxAmount = result.await().indefinitely();
        assertEquals(BigDecimal.ZERO, taxAmount); // No tax on zero amount
    }

    @Test
    void testCalculateTax_RoundingBehavior() {
        // Given
        Address address = new Address();
        address.country = "US";
        organization.billingAddress = address;

        Invoice testInvoice = new Invoice();
        testInvoice.organization = organization;
        testInvoice.subtotal = BigDecimal.valueOf(12.3456); // Amount that will need rounding

        // When
        Uni<BigDecimal> result = taxCalculator.calculateTax(testInvoice);

        // Then
        BigDecimal taxAmount = result.await().indefinitely();
        // 8% of 12.3456 = 0.987648, rounded to 2 decimals = 0.99
        assertEquals(BigDecimal.valueOf(0.99), taxAmount);
    }
}