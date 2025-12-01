/**
 * Timeline request
 */
record TimelineRequest(
    String userId,
    int maxResults,
    String pageToken
) {}