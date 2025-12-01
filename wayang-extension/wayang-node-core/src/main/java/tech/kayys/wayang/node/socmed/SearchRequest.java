/**
 * Search request
 */
record SearchRequest(
    String query,
    int maxResults,
    java.time.Instant since,
    java.time.Instant until
) {}