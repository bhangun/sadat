package tech.kayys.wayang.billing.dto;

import java.math.BigDecimal;

public record PlatformOverview(
    long totalOrganizations,
    long activeOrganizations,
    long totalSubscriptions,
    long activeSubscriptions,
    BigDecimal monthlyRecurringRevenue,
    BigDecimal annualRecurringRevenue,
    BigDecimal averageRevenuePerUser,
    double churnRate,
    long totalUsers,
    long totalWorkflowRuns
) {}