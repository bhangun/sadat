/**
 * Execution metadata
 */
record ExecutionMetadata(
    int attempt,
    int maxAttempts,
    Instant deadline,
    Priority priority,
    Map<String, String> tags
) {
    public ExecutionMetadata {
        tags = tags != null ? Map.copyOf(tags) : Map.of();
    }
}
