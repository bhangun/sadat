package tech.kayys.wayang.billing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.invoice.domain.Invoice;

@ApplicationScoped
public class TaxCalculator {

    public Uni<BigDecimal> calculateTax(Invoice invoice) {
        // Calculate tax based on organization location
        String country = invoice.organization.billingAddress != null ? invoice.organization.billingAddress.country
                : "US";

        BigDecimal taxRate = getTaxRate(country);
        BigDecimal taxableAmount = invoice.subtotal; // Assume subtotal is before tax

        BigDecimal taxAmount = taxableAmount.multiply(taxRate)
                .setScale(2, RoundingMode.HALF_UP);

        return Uni.createFrom().item(taxAmount);
    }

    private BigDecimal getTaxRate(String country) {
        // Simple tax rate lookup - in real implementation, this would be more complex
        return switch (country.toUpperCase()) {
            case "US" -> new BigDecimal("0.08"); // 8% for US
            case "CA" -> new BigDecimal("0.13"); // 13% for Canada (GST)
            case "GB" -> new BigDecimal("0.20"); // 20% for UK (VAT)
            case "DE" -> new BigDecimal("0.19"); // 19% for Germany (VAT)
            default -> BigDecimal.ZERO; // No tax for other countries
        };
    }
}