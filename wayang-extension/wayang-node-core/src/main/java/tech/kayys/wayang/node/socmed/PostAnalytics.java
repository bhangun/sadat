/**
 * Post analytics
 */
record PostAnalytics(
    String postId,
    int impressions,
    int engagements,
    int clicks,
    Map<String, Integer> demographics
) {}