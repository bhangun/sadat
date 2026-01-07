package tech.kayys.wayang.billing.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pricing tier with brackets
 */
public record PricingTier(
    BigDecimal basePrice,
    List<PricingBracket> brackets
) {}
