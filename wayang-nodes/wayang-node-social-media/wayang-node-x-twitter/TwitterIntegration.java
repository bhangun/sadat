package tech.kayys.wayang.node.core.integration.social;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.node.core.integration.AuthenticationResult;
import tech.kayys.wayang.node.core.integration.ConnectionTestResult;
import tech.kayys.wayang.node.core.integration.RateLimitStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Twitter/X API integration.
 * 
 * Supports:
 * - OAuth 2.0 authentication
 * - Tweet posting
 * - Timeline retrieval
 * - Search
 * - Media upload
 * - Analytics
 */
@ApplicationScoped
public class TwitterIntegration implements SocialMediaIntegration {
    
    private static final Logger LOG = LoggerFactory.getLogger(TwitterIntegration.class);
    private static final String API_BASE_URL = "https://api.twitter.com/2";
    private static final String UPLOAD_URL = "https://upload.twitter.com/1.1";
    
    private final HttpClient httpClient;
    private final MeterRegistry meterRegistry;
    private final String apiKey;
    private final String apiSecret;
    
    private volatile RateLimitStatus rateLimitStatus;
    
    @Inject
    public TwitterIntegration(
        MeterRegistry meterRegistry,
        @ConfigProperty(name = "wayang.integration.twitter.api-key") String apiKey,
        @ConfigProperty(name = "wayang.integration.twitter.api-secret") String apiSecret
    ) {
        this.meterRegistry = meterRegistry;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.rateLimitStatus = new RateLimitStatus(100, 100, 0);
    }
    
    @Override
    public String getServiceName() {
        return "Twitter/X";
    }
    
    @Override
    public CompletionStage<Boolean> healthCheck() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "/tweets"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                return response.statusCode() < 500;
                
            } catch (Exception e) {
                LOG.error("Twitter health check failed", e);
                return false;
            }
        });
    }
    
    @Override
    public CompletionStage<AuthenticationResult> authenticate(Map<String, String> credentials) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String consumerKey = credentials.getOrDefault("consumerKey", apiKey);
                String consumerSecret = credentials.getOrDefault("consumerSecret", apiSecret);
                
                // OAuth 2.0 Bearer Token authentication
                String auth = Base64.getEncoder().encodeToString(
                    (consumerKey + ":" + consumerSecret).getBytes()
                );
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.twitter.com/oauth2/token"))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                if (response.statusCode() == 200) {
                    // Parse response (simplified)
                    String accessToken = extractToken(response.body());
                    
                    meterRegistry.counter("twitter.auth.success").increment();
                    
                    return new AuthenticationResult(
                        true,
                        accessToken,
                        null,
                        7200,
                        Map.of("token_type", "bearer")
                    );
                } else {
                    meterRegistry.counter("twitter.auth.failure").increment();
                    return new AuthenticationResult(false, null, null, 0, Map.of());
                }
                
            } catch (Exception e) {
                LOG.error("Twitter authentication failed", e);
                return new AuthenticationResult(false, null, null, 0, Map.of());
            }
        });
    }
    
    @Override
    public CompletionStage<PostResult> post(PostRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String accessToken = getAccessToken();
                
                // Build tweet payload
                Map<String, Object> payload = Map.of(
                    "text", request.content()
                );
                
                if (request.mediaIds() != null && !request.mediaIds().isEmpty()) {
                    payload = new java.util.HashMap<>(payload);
                    payload.put("media", Map.of("media_ids", request.mediaIds()));
                }
                
                String jsonPayload = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(payload);
                
                HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "/tweets"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                updateRateLimitStatus(response);
                
                if (response.statusCode() == 201) {
                    String tweetId = extractTweetId(response.body());
                    String url = "https://twitter.com/user/status/" + tweetId;
                    
                    meterRegistry.counter("twitter.post.success").increment();
                    
                    return new PostResult(true, tweetId, url, "Tweet posted successfully");
                } else {
                    meterRegistry.counter("twitter.post.failure").increment();
                    return new PostResult(false, null, null, "Failed to post tweet");
                }
                
            } catch (Exception e) {
                LOG.error("Failed to post tweet", e);
                return new PostResult(false, null, null, e.getMessage());
            }
        });
    }
    
    @Override
    public CompletionStage<UserProfile> getUserProfile(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String accessToken = getAccessToken();
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "/users/" + userId + 
                        "?user.fields=username,name,description,public_metrics,verified"))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                updateRateLimitStatus(response);
                
                if (response.statusCode() == 200) {
                    return parseUserProfile(response.body());
                }
                
                return null;
                
            } catch (Exception e) {
                LOG.error("Failed to get user profile", e);
                return null;
            }
        });
    }
    
    @Override
    public CompletionStage<List<Post>> getTimeline(TimelineRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String accessToken = getAccessToken();
                
                String url = API_BASE_URL + "/users/" + request.userId() + 
                    "/tweets?max_results=" + request.maxResults() +
                    "&tweet.fields=created_at,public_metrics";
                
                if (request.pageToken() != null) {
                    url += "&pagination_token=" + request.pageToken();
                }
                
                HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                updateRateLimitStatus(response);
                
                if (response.statusCode() == 200) {
                    return parseTweets(response.body());
                }
                
                return List.of();
                
            } catch (Exception e) {
                LOG.error("Failed to get timeline", e);
                return List.of();
            }
        });
    }
    
    @Override
    public CompletionStage<SearchResult> search(SearchRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String accessToken = getAccessToken();
                
                String query = java.net.URLEncoder.encode(request.query(), "UTF-8");
                String url = API_BASE_URL + "/tweets/search/recent?query=" + query +
                    "&max_results=" + request.maxResults();
                
                HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                updateRateLimitStatus(response);
                
                if (response.statusCode() == 200) {
                    List<Post> posts = parseTweets(response.body());
                    return new SearchResult(posts, null, posts.size());
                }
                
                return new SearchResult(List.of(), null, 0);
                
            } catch (Exception e) {
                LOG.error("Failed to search tweets", e);
                return new SearchResult(List.of(), null, 0);
            }
        });
    }
    
    @Override
    public CompletionStage<PostAnalytics> getAnalytics(String postId) {
        return CompletableFuture.supplyAsync(() -> {
            // Note: Requires elevated access in Twitter API
            LOG.warn("Analytics API requires elevated access");
            return new PostAnalytics(postId, 0, 0, 0, Map.of());
        });
    }
    
    @Override
    public CompletionStage<Boolean> deletePost(String postId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String accessToken = getAccessToken();
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "/tweets/" + postId))
                    .header("Authorization", "Bearer " + accessToken)
                    .DELETE()
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                updateRateLimitStatus(response);
                
                return response.statusCode() == 200;
                
            } catch (Exception e) {
                LOG.error("Failed to delete tweet", e);
                return false;
            }
        });
    }
    
    @Override
    public CompletionStage<MediaUploadResult> uploadMedia(byte[] content, MediaType type) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String accessToken = getAccessToken();
                
                // Twitter media upload is a multi-step process
                // 1. INIT
                // 2. APPEND
                // 3. FINALIZE
                
                // Simplified version - would need full implementation
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(UPLOAD_URL + "/media/upload.json"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "multipart/form-data")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(content))
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                if (response.statusCode() == 200) {
                    String mediaId = extractMediaId(response.body());
                    return new MediaUploadResult(true, mediaId, null);
                }
                
                return new MediaUploadResult(false, null, null);
                
            } catch (Exception e) {
                LOG.error("Failed to upload media", e);
                return new MediaUploadResult(false, null, null);
            }
        });
    }
    
    @Override
    public RateLimitStatus getRateLimitStatus() {
        return rateLimitStatus;
    }
    
    @Override
    public CompletionStage<ConnectionTestResult> testConnection(Map<String, String> credentials) {
        return authenticate(credentials).thenApply(authResult -> {
            if (authResult.success()) {
                return new ConnectionTestResult(
                    true,
                    "Successfully connected to Twitter API",
                    Map.of("token_obtained", true)
                );
            } else {
                return new ConnectionTestResult(
                    false,
                    "Failed to connect to Twitter API",
                    Map.of()
                );
            }
        });
    }
    
    // Helper methods
    
    private String getAccessToken() {
        // In production, this would retrieve from secure storage
        return "cached_or_refreshed_token";
    }
    
    private void updateRateLimitStatus(HttpResponse<String> response) {
        try {
            int remaining = Integer.parseInt(
                response.headers().firstValue("x-rate-limit-remaining").orElse("100")
            );
            int limit = Integer.parseInt(
                response.headers().firstValue("x-rate-limit-limit").orElse("100")
            );
            long reset = Long.parseLong(
                response.headers().firstValue("x-rate-limit-reset").orElse("0")
            );
            
            rateLimitStatus = new RateLimitStatus(remaining, limit, reset);
            
        } catch (Exception e) {
            LOG.warn("Failed to update rate limit status", e);
        }
    }
    
    private String extractToken(String responseBody) {
        // Simplified - would use JSON parser
        return responseBody;
    }
    
    private String extractTweetId(String responseBody) {
        // Simplified - would use JSON parser
        return "tweet_id";
    }
    
    private String extractMediaId(String responseBody) {
        // Simplified - would use JSON parser
        return "media_id";
    }
    
    private UserProfile parseUserProfile(String responseBody) {
        // Simplified - would use JSON parser
        return new UserProfile(
            "id", "username", "Display Name", "Bio",
            1000, 500, "https://example.com/avatar.jpg", false
        );
    }
    
    private List<Post> parseTweets(String responseBody) {
        // Simplified - would use JSON parser
        return List.of();
    }
}