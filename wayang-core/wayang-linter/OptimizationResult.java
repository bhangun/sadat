@Value
@Builder
public class OptimizationResult {
    WorkflowDefinition original;
    WorkflowDefinition optimized;
    List<Optimization> optimizations;
    ImprovementMetrics estimatedImprovement;
}