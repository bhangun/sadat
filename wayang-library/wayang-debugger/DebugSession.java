
@Value
@Builder
public class DebugSession {
    String sessionId;
    UUID runId;
    Instant startTime;
    Instant endTime;
    DebugStatus status;
    Map<String, Object> metadata;
}