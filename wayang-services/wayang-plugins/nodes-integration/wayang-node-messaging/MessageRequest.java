/**
 * Message request
 */
record MessageRequest(
    String channel,
    String text,
    List<MessageBlock> blocks,
    List<MessageAttachment> attachments,
    String threadTs
) {}