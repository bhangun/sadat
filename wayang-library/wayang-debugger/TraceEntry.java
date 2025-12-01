@Value
@Builder
public class TraceEntry {
    String entryId;
    Instant timestamp;
    TraceLevel level;
    String source;
    String nodeId;
    String message;
    Map<String, Object> context;
    Optional<String> stackTrace;
}