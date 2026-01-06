package tech.kayys.wayang.node.core.integration.email;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Email service integration interface.
 */
public interface EmailIntegration {
    
    /**
     * Send email
     */
    CompletionStage<EmailSendResult> sendEmail(EmailMessage message);
    
    /**
     * Fetch emails
     */
    CompletionStage<List<Email>> fetchEmails(EmailFetchRequest request);
    
    /**
     * Search emails
     */
    CompletionStage<List<Email>> searchEmails(String query, int maxResults);
    
    /**
     * Mark email as read
     */
    CompletionStage<Boolean> markAsRead(String emailId);
    
    /**
     * Delete email
     */
    CompletionStage<Boolean> deleteEmail(String emailId);
    
    /**
     * Create draft
     */
    CompletionStage<String> createDraft(EmailMessage message);
}