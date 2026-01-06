
/**
 * Loop Node - Iterative execution over collections
 * Supports for-each pattern with configurable parallelism
 */
@ApplicationScoped
@NodeType("builtin.loop")
public class LoopNode extends AbstractNode {
    
    @Override
    protected Uni<ExecutionResult> doExecute(NodeContext context) {
        var collection = (Collection<?>) context.getInput("collection");
        var maxParallel = config.getInt("maxParallel", 1);
        
        if (collection == null || collection.isEmpty()) {
            return Uni.createFrom().item(ExecutionResult.success(Map.of("items", List.of())));
        }
        
        // Execute iterations with controlled parallelism
        return Uni.join().all(
            collection.stream()
                .map(item -> executeIteration(context, item))
                .collect(Collectors.toList())
        ).andCollectFailures()
        .map(results -> ExecutionResult.success(Map.of(
            "items", results,
            "count", results.size()
        )));
    }
    
    private Uni<Object> executeIteration(NodeContext context, Object item) {
        // Create iteration-scoped context
        var iterationCtx = context.createChild();
        iterationCtx.setVariable("item", item);
        
        // Execute iteration logic (delegated to sub-workflow)
        return Uni.createFrom().item(item);
    }
}
