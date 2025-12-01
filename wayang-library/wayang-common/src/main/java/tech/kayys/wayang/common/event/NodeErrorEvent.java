
public record NodeErrorEvent(
    String eventId,
    String runId,
    String nodeId,
    ErrorPayload error,
    Instant timestamp,
    String traceId
) implements WorkflowEvent {}