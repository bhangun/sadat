package tech.kayys.wayang.billing.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RevenueShare(
    UUID publisherId,
    java.time.YearMonth month,
    BigDecimal totalRevenue,
    BigDecimal publisherShare,
    BigDecimal platformFee,
    int numberOfListings
) {}