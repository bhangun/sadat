package tech.kayys.wayang.node.core.integration.messaging;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Messaging platform integration interface.
 */
public interface MessagingIntegration {
    
    /**
     * Send message to channel/user
     */
    CompletionStage<MessageResult> sendMessage(MessageRequest request);
    
    /**
     * Get channel messages
     */
    CompletionStage<List<Message>> getMessages(String channelId, int limit);
    
    /**
     * Create channel
     */
    CompletionStage<String> createChannel(String name, boolean isPrivate);
    
    /**
     * Upload file
     */
    CompletionStage<FileUploadResult> uploadFile(FileUpload file);
    
    /**
     * Get user info
     */
    CompletionStage<UserInfo> getUserInfo(String userId);
    
    /**
     * Set user status
     */
    CompletionStage<Boolean> setStatus(String status, String emoji);
}