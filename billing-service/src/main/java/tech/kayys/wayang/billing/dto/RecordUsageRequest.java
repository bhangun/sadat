package tech.kayys.wayang.billing.dto;

import java.util.Map;

import jakarta.validation.constraints.NotNull;
import tech.kayys.wayang.billing.model.UsageType;

public record RecordUsageRequest(
    @NotNull String tenantId,
    @NotNull UsageType usageType,
    long quantity,
    String unit,
    String resourceId,
    Map<String, Object> metadata
) {}
