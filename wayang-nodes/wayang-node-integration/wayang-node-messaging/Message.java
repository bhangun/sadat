/**
 * Message
 */
record Message(
    String id,
    String userId,
    String text,
    java.time.Instant timestamp,
    List<String> reactions
) {}