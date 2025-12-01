@Value
@Builder
public class ExecutionResult {
    Status status;
    Map<String, Object> outputs;
    List<Event> events;
    List<LogEntry> logs;
    ExecutionMetrics metrics;
    Optional<String> checkpointRef;
    Optional<String> error;
}