/**
 * Search result
 */
record SearchResult(
    List<Post> posts,
    String nextPageToken,
    int totalResults
) {}