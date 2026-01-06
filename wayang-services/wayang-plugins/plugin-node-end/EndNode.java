
/**
 * End Node - Terminal node for workflow completion
 * Collects final outputs and triggers cleanup
 */
@ApplicationScoped
@NodeType("builtin.end")
public class EndNode extends AbstractNode {
    
    @Override
    protected Uni<ExecutionResult> doExecute(NodeContext context) {
        return Uni.createFrom().item(() -> {
            var finalOutputs = context.getAllInputs();
            
            // Emit workflow completion event
            context.emitEvent("workflow.completed", Map.of(
                "runId", context.getRunId(),
                "outputs", finalOutputs,
                "timestamp", Instant.now()
            ));
            
            return ExecutionResult.success(finalOutputs);
        });
    }
}
