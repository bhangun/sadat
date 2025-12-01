
/**
 * Email fetch request
 */
record EmailFetchRequest(
    int maxResults,
    String pageToken,
    boolean unreadOnly,
    List<String> labels
) {}