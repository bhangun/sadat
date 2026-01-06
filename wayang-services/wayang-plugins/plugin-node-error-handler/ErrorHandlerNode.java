
/**
 * ==============================================
 * ERROR HANDLING & OBSERVABILITY NODES
 * ==============================================
 */

/**
 * Error Handler Node - Process and route errors
 * Implements retry, fallback, auto-fix, and escalation logic
 */
@ApplicationScoped
@NodeType("builtin.error.handler")
public class ErrorHandlerNode extends AbstractNode {
    
    @Override
    protected Uni<ExecutionResult> doExecute(NodeContext context) {
        var error = (ErrorPayload) context.getInput("error");
        var rules = config.getList("rules", Map.class);
        
        return Uni.createFrom().item(() -> {
            // Evaluate rules to determine action
            for (var rule : rules) {
                var condition = (String) rule.get("when");
                var action = (String) rule.get("action");
                
                if (evaluateErrorCondition(error, condition)) {
                    return ExecutionResult.success(Map.of(
                        "action", action,
                        "error", error,
                        "rule", rule.get("name")
                    ));
                }
            }
            
            // Default action
            return ExecutionResult.success(Map.of(
                "action", "abort",
                "error", error
            ));
        });
    }
    
    private boolean evaluateErrorCondition(ErrorPayload error, String condition) {
        var celEngine = CelEngine.create();
        var context = Map.of("error", error);
        
        try {
            var result = celEngine.evaluate(condition, context);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            context.getLogger().warn("Error condition evaluation failed", e);
            return false;
        }
    }
}
