/**
 * Email message
 */
record EmailMessage(
    String to,
    List<String> cc,
    List<String> bcc,
    String subject,
    String body,
    boolean isHtml,
    List<EmailAttachment> attachments,
    Map<String, String> headers
) {}