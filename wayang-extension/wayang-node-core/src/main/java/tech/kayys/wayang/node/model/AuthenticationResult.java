/**
 * Authentication result
 */
record AuthenticationResult(
    boolean success,
    String accessToken,
    String refreshToken,
    long expiresIn,
    Map<String, Object> metadata
) {}