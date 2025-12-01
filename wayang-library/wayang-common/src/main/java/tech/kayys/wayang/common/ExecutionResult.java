
@RegisterForReflection
public record ExecutionResult(
    Status status,
    Map<String, Object> outputs,
    ErrorPayload error,
    List<AuditPayload> events,
    ExecutionMetrics metrics
) {
    public enum Status {
        SUCCESS, ERROR, TIMEOUT, RETRY_NEEDED
    }
    
    public static ExecutionResult success(Map<String, Object> outputs) {
        return new ExecutionResult(Status.SUCCESS, outputs, null, List.of(), null);
    }
    
    public static ExecutionResult error(ErrorPayload error) {
        return new ExecutionResult(Status.ERROR, Map.of(), error, List.of(), null);
    }
}