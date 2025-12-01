@Value
@Builder
public class PlanningHints {
    PlanningMode mode;
    Duration latencyBudget;
    double costBudget;
    double riskTolerance;
    List<String> preferredModels;
}