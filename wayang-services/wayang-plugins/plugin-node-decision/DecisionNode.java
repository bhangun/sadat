
/**
 * Decision Node - Conditional branching using CEL expressions
 * Routes execution based on evaluated conditions
 */
@ApplicationScoped
@NodeType("builtin.decision")
public class DecisionNode extends AbstractNode {
    
    private CelEngine celEngine;
    
    @Override
    protected void doOnLoad(NodeDescriptor descriptor, NodeConfig config) {
        this.celEngine = CelEngine.create();
    }
    
    @Override
    protected Uni<ExecutionResult> doExecute(NodeContext context) {
        return Uni.createFrom().item(() -> {
            var condition = config.getString("condition");
            var celContext = buildCelContext(context);
            
            try {
                var result = celEngine.evaluate(condition, celContext);
                var branch = Boolean.TRUE.equals(result) ? "true" : "false";
                
                return ExecutionResult.success(Map.of(
                    "branch", branch,
                    "condition", condition,
                    "result", result
                ));
            } catch (Exception e) {
                throw new ValidationException("CEL evaluation failed: " + e.getMessage(), e);
            }
        });
    }
    
    private Map<String, Object> buildCelContext(NodeContext context) {
        var celCtx = new HashMap<String, Object>();
        celCtx.putAll(context.getAllInputs());
        celCtx.put("metadata", context.getMetadata());
        return celCtx;
    }
}
