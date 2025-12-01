record PlanEstimate(
    int estimatedTokens,
    double estimatedCostUSD,
    long estimatedDurationMs,
    double riskScore,
    List<String> requiredApprovals
) {}