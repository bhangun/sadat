
/**
 * Human-in-the-Loop Node - Pause for human review
 * Creates approval tasks and waits for human decision
 */
@ApplicationScoped
@NodeType("builtin.hitl")
public class HumanReviewNode extends AbstractNode {
    
    @Inject
    HITLService hitlService;
    
    @Override
    protected Uni<ExecutionResult> doExecute(NodeContext context) {
        var data = context.getAllInputs();
        var reviewType = config.getString("reviewType", "approval");
        var timeout = config.getInt("timeout", 3600000); // 1 hour default
        
        var task = HITLTask.builder()
            .type(reviewType)
            .data(data)
            .runId(context.getRunId())
            .nodeId(context.getNodeId())
            .tenantId(context.getTenantId())
            .timeout(timeout)
            .build();
        
        return hitlService.createTask(task)
            .flatMap(taskId -> hitlService.waitForCompletion(taskId))
            .map(decision -> ExecutionResult.success(Map.of(
                "decision", decision.getAction(),
                "reviewer", decision.getReviewer(),
                "notes", decision.getNotes(),
                "correctedData", decision.getCorrectedData()
            )));
    }
    
    @Override
    public Optional<CheckpointState> checkpoint(NodeContext context) {
        // Support resumability after human review
        return Optional.of(CheckpointState.of(
            "awaiting_human_review",
            context.getAllInputs()
        ));
    }
}
