package tech.kayys.wayang.billing.dto;

import java.util.Map;

import tech.kayys.wayang.billing.model.UsageType;

public record UsageMetrics(
    long totalWorkflowExecutions,
    long totalAiTokens,
    long totalApiCalls,
    long totalStorageGb,
    Map<UsageType, Long> usageByType
) {}