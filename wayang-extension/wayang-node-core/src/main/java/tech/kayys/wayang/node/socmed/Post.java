/**
 * Post/Tweet
 */
record Post(
    String id,
    String authorId,
    String content,
    java.time.Instant createdAt,
    int likesCount,
    int sharesCount,
    int commentsCount,
    List<String> mediaUrls
) {}