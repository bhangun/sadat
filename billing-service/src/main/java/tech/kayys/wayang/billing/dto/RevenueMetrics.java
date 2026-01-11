package tech.kayys.wayang.billing.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

public record RevenueMetrics(
    Map<YearMonth, BigDecimal> monthlyRevenue,
    Map<String, BigDecimal> revenueByPlan,
    BigDecimal newSubscriptionsRevenue,
    BigDecimal churnedRevenue,
    BigDecimal expansionRevenue
) {}