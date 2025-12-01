@ApplicationScoped
@Named("symbolic")
public class SymbolicPlanner implements PlanningStrategy {
    @Inject RuleEngine ruleEngine;
    
    @Override
    public ActionGraph plan(Goal goal, PlanningContext context) {
        // Use rule-based planning for deterministic tasks
        return ruleEngine.plan(goal, context);
    }
}