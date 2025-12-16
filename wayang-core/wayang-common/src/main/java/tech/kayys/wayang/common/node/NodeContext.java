
@RegisterForReflection
public record NodeContext(
    String runId,
    String nodeId,
    String tenantId,
    Map<String, Object> inputs,
    Map<String, Object> variables,
    ExecutionMetadata metadata
) {
    public Object getInput(String name) {
        return inputs.get(name);
    }
    
    public <T> T getInput(String name, Class<T> type) {
        return type.cast(inputs.get(name));
    }
}