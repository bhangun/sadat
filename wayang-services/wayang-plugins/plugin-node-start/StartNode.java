
/**
 * Start Node - Entry point for workflow execution
 * Validates initial inputs and sets up execution context
 */
@ApplicationScoped
@NodeType("builtin.start")
public class StartNode extends AbstractNode {
    
    @Override
    protected Uni<ExecutionResult> doExecute(NodeContext context) {
        return Uni.createFrom().item(() -> {
            // Validate workflow-level inputs
            var inputs = context.getAllInputs();
            
            // Emit workflow start event
            context.emitEvent("workflow.started", Map.of(
                "runId", context.getRunId(),
                "inputs", inputs,
                "timestamp", Instant.now()
            ));
            
            // Pass through inputs to next nodes
            return ExecutionResult.success(inputs);
        });
    }
}
