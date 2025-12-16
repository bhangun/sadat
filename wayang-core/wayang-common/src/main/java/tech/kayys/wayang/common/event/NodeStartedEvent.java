
public record NodeStartedEvent(
    String eventId,
    String runId,
    String nodeId,
    Instant timestamp,
    String traceId
) implements WorkflowEvent {}

