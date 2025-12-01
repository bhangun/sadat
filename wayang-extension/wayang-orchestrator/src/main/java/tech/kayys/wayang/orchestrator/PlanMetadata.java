@Value
@Builder
public class PlanMetadata {
    int estimatedTokens;
    double estimatedCostUSD;
    double riskScore;
    Duration estimatedDuration;
    List<String> requiredApprovals;
    Map<String, String> tags;
}