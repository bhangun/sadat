package tech.kayys.wayang.organization.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record OrganizationStats(
    UUID organizationId,
    long totalUsers,
    long activeWorkflows,
    long totalWorkflowRuns,
    BigDecimal currentMonthUsageCost,
    Map<String, Long> resourceUtilization
) {}
