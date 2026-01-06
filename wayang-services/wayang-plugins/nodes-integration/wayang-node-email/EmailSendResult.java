/**
 * Email send result
 */
record EmailSendResult(
    boolean success,
    String messageId,
    String message
) {}