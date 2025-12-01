
@ApplicationScoped
@Named("tot")
public class TreeOfThoughtPlanner implements PlanningStrategy {
    @Inject LLMAdapter llmAdapter;
    @Inject Evaluator evaluator;
    
    @Override
    public ActionGraph plan(Goal goal, PlanningContext context) {
        // Generate multiple plan branches
        List<ActionGraph> candidates = generateCandidates(goal, context);
        
        // Evaluate each branch
        List<ScoredPlan> scored = candidates.stream()
            .map(graph -> new ScoredPlan(
                graph,
                evaluator.evaluate(graph, context)
            ))
            .collect(Collectors.toList());
        
        // Return best plan
        return scored.stream()
            .max(Comparator.comparing(ScoredPlan::getScore))
            .map(ScoredPlan::getGraph)
            .orElseThrow();
    }
}