
/**
 * Error information
 */
record ErrorInfo(
    String code,
    String message,
    String category,
    boolean retryable,
    Map<String, Object> details,
    String stackTrace
) {
    public ErrorInfo {
        details = details != null ? Map.copyOf(details) : Map.of();
    }
}
