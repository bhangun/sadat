package tech.kayys.wayang.agent;

/**
 * Built-in Planner - Creates execution plans
 */
public record BuiltInPlanner(
    PlanningStrategy strategy,
    boolean enableAdaptivePlanning,
    int maxReplanAttempts
) {
    
    public static BuiltInPlanner createDefault() {
        return new BuiltInPlanner(
            PlanningStrategy.PLAN_AND_EXECUTE,
            true,
            3
        );
    }
}
