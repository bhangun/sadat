
/**
 * Execution result from node
 */
@Immutable
public final class ExecutionResult {
    private final ExecutionStatus status;
    private final Map<String, Object> outputs;
    private final List<Event> events;
    private final Optional<String> errorMessage;
    private final ExecutionMetrics metrics;
    
    // Implementation...
    
    public static ExecutionResult success(Map<String, Object> outputs) {
        return new Builder()
                .status(ExecutionStatus.SUCCESS)
                .outputs(outputs)
                .build();
    }
    
    public static ExecutionResult failure(String message) {
        return new Builder()
                .status(ExecutionStatus.FAILED)
                .errorMessage(message)
                .build();
    }
}