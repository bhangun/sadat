
/**
 * Merge Node - Combines multiple input branches
 * Supports various merge strategies (all, any, first)
 */
@ApplicationScoped
@NodeType("builtin.merge")
public class MergeNode extends AbstractNode {
    
    @Override
    protected Uni<ExecutionResult> doExecute(NodeContext context) {
        return Uni.createFrom().item(() -> {
            var strategy = config.getString("strategy", "all");
            var inputs = context.getAllInputs();
            
            var merged = switch (strategy) {
                case "all" -> mergeAll(inputs);
                case "first" -> mergeFirst(inputs);
                case "any" -> mergeAny(inputs);
                default -> throw new ValidationException("Unknown merge strategy: " + strategy);
            };
            
            return ExecutionResult.success(Map.of("merged", merged));
        });
    }
    
    private Map<String, Object> mergeAll(Map<String, Object> inputs) {
        // Wait for all branches, merge outputs
        var merged = new HashMap<String, Object>();
        inputs.forEach((key, value) -> {
            if (value instanceof Map) {
                merged.putAll((Map<String, Object>) value);
            }
        });
        return merged;
    }
    
    private Map<String, Object> mergeFirst(Map<String, Object> inputs) {
        // Return first non-null input
        return inputs.values().stream()
            .filter(v -> v != null)
            .findFirst()
            .map(v -> v instanceof Map ? (Map<String, Object>) v : Map.of("value", v))
            .orElse(Map.of());
    }
    
    private Map<String, Object> mergeAny(Map<String, Object> inputs) {
        // Return any non-null input
        return mergeFirst(inputs);
    }
}
