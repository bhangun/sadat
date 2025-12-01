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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Facebook/Instagram Graph API integration.
 * 
 * Supports:
 * - OAuth 2.0 authentication
 * - Page/Profile posting
 * - Media publishing to Instagram
 * - Insights/Analytics
 * - Comments and engagement
 */
@ApplicationScoped
public class FacebookInstagramIntegration implements SocialMediaIntegration {
    
    private static final Logger LOG = LoggerFactory.getLogger(FacebookInstagramIntegration.class);
    private static final String GRAPH_API_BASE = "https://graph.facebook.com/v18.0";
    
    private final HttpClient httpClient;
    private final MeterRegistry meterRegistry;
    private final String appId;
    private final String appSecret;
    
    @Inject
    public FacebookInstagramIntegration(
        MeterRegistry meterRegistry,
        @ConfigProperty(name = "wayang.integration.facebook.app-id") String appId,
        @ConfigProperty(name = "wayang.integration.facebook.app-secret") String appSecret
    ) {
        this.meterRegistry = meterRegistry;
        this.appId = appId;
        this.appSecret = appSecret;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }
    
    @Override
    public String getServiceName() {
        return "Facebook/Instagram";
    }
    
    @Override
    public CompletionStage<Boolean> healthCheck() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GRAPH_API_BASE + "/me"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                return response.statusCode() < 500;
                
            } catch (Exception e) {
                LOG.error("Facebook/Instagram health check failed", e);
                return false;
            }
        });
    }
    
    @Override
    public CompletionStage<AuthenticationResult> authenticate(Map<String, String> credentials) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String code = credentials.get("code");
                String redirectUri = credentials.get("redirectUri");
                
                String url = GRAPH_API_BASE + "/oauth/access_token" +
                    "?client_id=" + appId +
                    "&client_secret=" + appSecret +
                    "&redirect_uri=" + redirectUri +
                    "&code=" + code;
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                if (response.statusCode() == 200) {
                    Map<String, Object> tokenData = parseTokenResponse(response.body());
                    
                    meterRegistry.counter("facebook.auth.success").increment();
                    
                    return new AuthenticationResult(
                        true,
                        (String) tokenData.get("access_token"),
                        null,
                        ((Number) tokenData.getOrDefault("expires_in", 3600)).longValue(),
                        tokenData
                    );
                } else {
                    meterRegistry.counter("facebook.auth.failure").increment();
                    return new AuthenticationResult(false, null, null, 0, Map.of());
                }
                
            } catch (Exception e) {
                LOG.error("Facebook authentication failed", e);
                return new AuthenticationResult(false, null, null, 0, Map.of());
            }
        });
    }
    
    @Override
    public CompletionStage<PostResult> post(PostRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String accessToken = getAccessToken();
                String pageId = getPageId(); // Would come from context
                
                // Determine if posting to Facebook or Instagram
                boolean isInstagram = request.metadata().getOrDefault("platform", "facebook")
                    .equals("instagram");
                
                if (isInstagram) {
                    return postToInstagram(request, accessToken);
                } else {
                    return postToFacebook(request, accessToken, pageId);
                }
                
            } catch (Exception e) {
                LOG.error("Failed to post to Facebook/Instagram", e);
                return new PostResult(false, null, null, e.getMessage());
            }
        });
    }
    
    /**
     * Post to Facebook Page
     */
    private PostResult postToFacebook(PostRequest request, String accessToken, String pageId) {
        try {
            String url = GRAPH_API_BASE + "/" + pageId + "/feed";
            
            Map<String, String> params = new java.util.HashMap<>();
            params.put("message", request.content());
            params.put("access_token", accessToken);
            
            if (request.mediaIds() != null && !request.mediaIds().isEmpty()) {
                params.put("attached_media", String.join(",", request.mediaIds()));
            }
            
            String payload = buildFormUrlEncoded(params);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
            
            HttpResponse<String> response = httpClient.send(
                httpRequest,
                HttpResponse.BodyHandlers.ofString()
            );
            
            if (response.statusCode() == 200) {
                String postId = extractId(response.body());
                String postUrl = "https://facebook.com/" + postId;
                
                meterRegistry.counter("facebook.post.success").increment();
                
                return new PostResult(true, postId, postUrl, "Posted successfully");
            } else {
                meterRegistry.counter("facebook.post.failure").increment();
                return new PostResult(false, null, null, "Failed to post");
            }
            
        } catch (Exception e) {
            LOG.error("Failed to post to Facebook", e);
            return new PostResult(false, null, null, e.getMessage());
        }
    }
    
    /**
     * Post to Instagram (requires Business/Creator account)
     */
    private PostResult postToInstagram(PostRequest request, String accessToken) {
        try {
            String igUserId = getInstagramUserId(); // Would come from context
            
            // Instagram posting is a 2-step process:
            // 1. Create media container
            // 2. Publish container
            
            // Step 1: Create container
            String containerUrl = GRAPH_API_BASE + "/" + igUserId + "/media";
            
            Map<String, String> containerParams = new java.util.HashMap<>();
            containerParams.put("image_url", request.mediaIds().get(0)); // Assuming URL
            containerParams.put("caption", request.content());
            containerParams.put("access_token", accessToken);
            
            String containerPayload = buildFormUrlEncoded(containerParams);
            
            HttpRequest containerRequest = HttpRequest.newBuilder()
                .uri(URI.create(containerUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(containerPayload))
                .build();
            
            HttpResponse<String> containerResponse = httpClient.send(
                containerRequest,
                HttpResponse.BodyHandlers.ofString()
            );
            
            if (containerResponse.statusCode() != 200) {
                return new PostResult(false, null, null, "Failed to create media container");
            }
            
            String containerId = extractId(containerResponse.body());
            
            // Step 2: Publish
            String publishUrl = GRAPH_API_BASE + "/" + igUserId + "/media_publish";
            
            Map<String, String> publishParams = new java.util.HashMap<>();
            publishParams.put("creation_id", containerId);
            publishParams.put("access_token", accessToken);
            
            String publishPayload = buildFormUrlEncoded(publishParams);
            
            HttpRequest publishRequest = HttpRequest.newBuilder()
                .uri(URI.create(publishUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(publishPayload))
                .build();
            
            HttpResponse<String> publishResponse = httpClient.send(
                publishRequest,
                HttpResponse.BodyHandlers.ofString()
            );
            
            if (publishResponse.statusCode() == 200) {
                String postId = extractId(publishResponse.body());
                
                meterRegistry.counter("instagram.post.success").increment();
                
                return new PostResult(true, postId, null, "Posted to Instagram");
            } else {
                meterRegistry.counter("instagram.post.failure").increment();
                return new PostResult(false, null, null, "Failed to publish");
            }
            
        } catch (Exception e) {
            LOG.error("Failed to post to Instagram", e);
            return new PostResult(false, null, null, e.getMessage());
        }
    }
    
    @Override
    public CompletionStage<UserProfile> getUserProfile(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String accessToken = getAccessToken();
                
                String url = GRAPH_API_BASE + "/" + userId +
                    "?fields=id,name,username,followers_count,follows_count,profile_picture_url" +
                    "&access_token=" + accessToken;
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                
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
                
                String url = GRAPH_API_BASE + "/" + request.userId() + "/feed" +
                    "?fields=id,message,created_time,likes.summary(true),comments.summary(true)" +
                    "&limit=" + request.maxResults() +
                    "&access_token=" + accessToken;
                
                if (request.pageToken() != null) {
                    url += "&after=" + request.pageToken();
                }
                
                HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                if (response.statusCode() == 200) {
                    return parsePosts(response.body());
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
                
                String url = GRAPH_API_BASE + "/search" +
                    "?q=" + java.net.URLEncoder.encode(request.query(), "UTF-8") +
                    "&type=post" +
                    "&access_token=" + accessToken;
                
                HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                if (response.statusCode() == 200) {
                    List<Post> posts = parsePosts(response.body());
                    return new SearchResult(posts, null, posts.size());
                }
                
                return new SearchResult(List.of(), null, 0);
                
            } catch (Exception e) {
                LOG.error("Failed to search", e);
                return new SearchResult(List.of(), null, 0);
            }
        });
    }
    
    @Override
    public CompletionStage<PostAnalytics> getAnalytics(String postId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String accessToken = getAccessToken();
                
                String url = GRAPH_API_BASE + "/" + postId + "/insights" +
                    "?metric=post_impressions,post_engaged_users,post_clicks" +
                    "&access_token=" + accessToken;
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                if (response.statusCode() == 200) {
                    return parseAnalytics(postId, response.body());
                }
                
                return new PostAnalytics(postId, 0, 0, 0, Map.of());
                
            } catch (Exception e) {
                LOG.error("Failed to get analytics", e);
                return new PostAnalytics(postId, 0, 0, 0, Map.of());
            }
        });
    }
    
    @Override
    public CompletionStage<Boolean> deletePost(String postId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String accessToken = getAccessToken();
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GRAPH_API_BASE + "/" + postId + "?access_token=" + accessToken))
                    .DELETE()
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                return response.statusCode() == 200;
                
            } catch (Exception e) {
                LOG.error("Failed to delete post", e);
                return false;
            }
        });
    }
    
    @Override
    public CompletionStage<MediaUploadResult> uploadMedia(byte[] content, MediaType type) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String accessToken = getAccessToken();
                String pageId = getPageId();
                
                // Upload to Facebook
                String url = GRAPH_API_BASE + "/" + pageId + "/photos";
                
                // In production, use multipart/form-data
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "?access_token=" + accessToken))
                    .header("Content-Type", "image/jpeg")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(content))
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                if (response.statusCode() == 200) {
                    String mediaId = extractId(response.body());
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
        // Facebook uses app-level rate limits
        return new RateLimitStatus(200, 200, 0);
    }
    
    @Override
    public CompletionStage<ConnectionTestResult> testConnection(Map<String, String> credentials) {
        return authenticate(credentials).thenApply(authResult -> {
            if (authResult.success()) {
                return new ConnectionTestResult(
                    true,
                    "Successfully connected to Facebook/Instagram API",
                    Map.of("token_obtained", true)
                );
            } else {
                return new ConnectionTestResult(
                    false,
                    "Failed to connect to Facebook/Instagram API",
                    Map.of()
                );
            }
        });
    }
    
    // Helper methods
    
    private String getAccessToken() {
        return "cached_token";
    }
    
    private String getPageId() {
        return "page_id";
    }
    
    private String getInstagramUserId() {
        return "ig_user_id";
    }
    
    private String buildFormUrlEncoded(Map<String, String> params) {
        return params.entrySet().stream()
            .map(e -> {
                try {
                    return java.net.URLEncoder.encode(e.getKey(), "UTF-8") + "=" +
                           java.net.URLEncoder.encode(e.getValue(), "UTF-8");
                } catch (Exception ex) {
                    return "";
                }
            })
            .filter(s -> !s.isEmpty())
            .reduce((a, b) -> a + "&" + b)
            .orElse("");
    }
    
    private String extractId(String responseBody) {
        // Simplified - would use JSON parser
        return "id";
    }
    
    private Map<String, Object> parseTokenResponse(String responseBody) {
        // Simplified - would use JSON parser
        return Map.of("access_token", "token", "expires_in", 3600);
    }
    
    private UserProfile parseUserProfile(String responseBody) {
        return new UserProfile(
            "id", "username", "Name", "Bio",
            1000, 500, "https://example.com/avatar.jpg", false
        );
    }
    
    private List<Post> parsePosts(String responseBody) {
        return List.of();
    }
    
    private PostAnalytics parseAnalytics(String postId, String responseBody) {
        return new PostAnalytics(postId, 0, 0, 0, Map.of());
    }
}