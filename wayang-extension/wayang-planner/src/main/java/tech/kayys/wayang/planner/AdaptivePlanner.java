
@ApplicationScoped
public class AdaptivePlanner implements PlannerEngine {
    @Inject IntentParser intentParser;
    @Inject ContextFetcher contextFetcher;
    @Inject StrategySelector strategySelector;
    @Inject PlannerCore plannerCore;
    @Inject NodeMapper nodeMapper;
    @Inject PlanValidator validator;
    @Inject CostEstimator costEstimator;
    @Inject PlanVersioner versioner;
    
    @Override
    public ExecutionPlan plan(PlanningRequest request) {
        // Parse intent
        Goal goal = intentParser.parse(request.getIntent());
        
        // Fetch context
        PlanningContext context = contextFetcher.fetch(goal, request);
        
        // Select strategy
        PlanningStrategy strategy = strategySelector.select(goal, context);
        
        // Generate plan
        ActionGraph actionGraph = plannerCore.plan(goal, context, strategy);
        
        // Map to nodes
        ExecutionPlan plan = nodeMapper.mapToNodes(actionGraph, context);
        
        // Validate
        PlanValidation validation = validator.validate(plan, context);
        if (!validation.isValid()) {
            throw new InvalidPlanException(validation.getErrors());
        }
        
        // Estimate cost
        PlanEstimate estimate = costEstimator.estimate(plan, context);
        plan = plan.withMetadata(
            plan.getMetadata().withEstimate(estimate)
        );
        
        // Version and persist
        plan = versioner.version(plan);
        
        return plan;
    }
}