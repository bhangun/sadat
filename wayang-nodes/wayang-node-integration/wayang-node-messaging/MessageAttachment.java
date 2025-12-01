/**
 * Message attachment
 */
record MessageAttachment(
    String title,
    String text,
    String color,
    List<AttachmentField> fields
) {}