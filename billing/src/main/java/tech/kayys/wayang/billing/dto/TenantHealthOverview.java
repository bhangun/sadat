package tech.kayys.wayang.billing.dto;

import java.util.Map;

import tech.kayys.wayang.organization.model.OrganizationStatus;

public record TenantHealthOverview(
    long healthyTenants,
    long atRiskTenants,
    Map<OrganizationStatus, Long> tenantsByStatus,
    double averageUptime
) {}
