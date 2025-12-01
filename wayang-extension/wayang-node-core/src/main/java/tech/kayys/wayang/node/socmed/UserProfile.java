/**
 * User profile
 */
record UserProfile(
    String userId,
    String username,
    String displayName,
    String bio,
    int followersCount,
    int followingCount,
    String profileImageUrl,
    boolean verified
) {}