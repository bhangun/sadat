package tech.kayys.wayang.billing.model;

import java.math.BigDecimal;

/**
 * Pricing bracket for tiered pricing
 */
public record PricingBracket(
    long from,
    long to,
    BigDecimal price
) {}
