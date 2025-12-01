
/**
 * Rate limit status
 */

record RateLimitStatus(
    int remaining,
    int limit,
    long resetTimeEpochSeconds
) {}