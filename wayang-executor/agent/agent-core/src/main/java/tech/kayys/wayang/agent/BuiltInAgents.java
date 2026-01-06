package tech.kayys.wayang.agent;

/**
 * Built-in agents for orchestrator
 */
public record BuiltInAgents(
    BuiltInPlanner planner,
    BuiltInExecutor executor,
    BuiltInEvaluator evaluator
) {
    
    public static BuiltInAgents createDefault() {
        return new BuiltInAgents(
            BuiltInPlanner.createDefault(),
            BuiltInExecutor.createDefault(),
            BuiltInEvaluator.createDefault()
        );
    }
}
