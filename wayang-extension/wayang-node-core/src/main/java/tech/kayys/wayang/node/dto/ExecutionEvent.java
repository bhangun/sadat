/**
 * Execution event
 */
record ExecutionEvent(
    String type,
    Instant timestamp,
    String message,
    Map<String, Object> data
) {
    public ExecutionEvent {
        data = data != null ? Map.copyOf(data) : Map.of();
    }
}