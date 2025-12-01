/**
 * Email attachment
 */
record EmailAttachment(
    String filename,
    String mimeType,
    byte[] content
) {}