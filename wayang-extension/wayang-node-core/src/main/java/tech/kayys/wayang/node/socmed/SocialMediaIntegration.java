package tech.kayys.wayang.node.core.integration.social;

import tech.kayys.wayang.node.core.integration.ExternalServiceIntegration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Interface for social media platform integrations.
 */
public interface SocialMediaIntegration extends ExternalServiceIntegration {
    
    /**
     * Post content to the platform
     */
    CompletionStage<PostResult> post(PostRequest request);
    
    /**
     * Get user profile
     */
    CompletionStage<UserProfile> getUserProfile(String userId);
    
    /**
     * Get posts/tweets from user timeline
     */
    CompletionStage<List<Post>> getTimeline(TimelineRequest request);
    
    /**
     * Search for content
     */
    CompletionStage<SearchResult> search(SearchRequest request);
    
    /**
     * Get post analytics
     */
    CompletionStage<PostAnalytics> getAnalytics(String postId);
    
    /**
     * Delete a post
     */
    CompletionStage<Boolean> deletePost(String postId);
    
    /**
     * Upload media (images, videos)
     */
    CompletionStage<MediaUploadResult> uploadMedia(byte[] content, MediaType type);
}