
/**
 * Email
 */
record Email(
    String id,
    String from,
    List<String> to,
    String subject,
    String body,
    java.time.Instant receivedAt,
    boolean isRead,
    List<String> labels
) {}