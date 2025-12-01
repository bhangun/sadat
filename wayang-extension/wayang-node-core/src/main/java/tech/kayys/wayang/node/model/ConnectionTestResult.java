/**
 * Connection test result
 */
record ConnectionTestResult(
    boolean success,
    String message,
    Map<String, Object> details
) {}