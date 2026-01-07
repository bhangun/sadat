package tech.kayys.wayang.billing.model;

public record UsageProfile(
    long totalUsage,
    double avgDailyUsage,
    double intensityScore,
    int eventCount
) {}
