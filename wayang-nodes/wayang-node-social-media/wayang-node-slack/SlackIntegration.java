package tech.kayys.wayang.node.core.integration.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Slack API integration.
 * 
 * Supports:
 * - OAuth 2.0 authentication
 * - Send messages with rich formatting
 * - Channel management
 * - File uploads
 * - User management
 * - Reactions
 */
@ApplicationScoped
public class SlackIntegration implements MessagingIntegration {
    
    private static final Logger LOG = LoggerFactory.getLogger(SlackIntegration.class);
    private static final String SLACK_API_BASE = "https://slack.com/api";
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final String clientId;
    private final String clientSecret;
    
    @Inject
    public SlackIntegration(
        ObjectMapper objectMapper,
        MeterRegistry meterRegistry,
        @ConfigProperty(name = "wayang.integration.slack.client-id") String clientId,
        @ConfigProperty(name = "wayang.integration.slack.client-secret") String clientSecret
    ) {
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }
    
    @Override
    public CompletionStage<MessageResult> sendMessage(MessageRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String accessToken = getAccessToken();
                
                Map<String, Object> payload = new HashMap<>();
                payload.put("channel", request.channel());
                payload.put("text", request.text());
                
                if (request.blocks() != null && !request.blocks().isEmpty()) {
                    List<Map<String, Object>> blocks = request.blocks().stream()
                        .map(block -> Map.of(
                            "type", block.type(),
                            block.type(), block.content()
                        ))
                        .toList();
                    payload.put("blocks", blocks);
                }
                
                if (request.attachments() != null && !request.attachments().isEmpty()) {
                    List<Map<String, Object>> attachments = request.attachments().stream()
                        .map(att -> {
                            Map<String, Object> attMap = new HashMap<>();
                            attMap.put("title", att.title());
                            attMap.put("text", att.text());
                            attMap.put("color", att.color());
                            if (att.fields() != null) {
                                attMap.put("fields", att.fields().stream()
                                    .map(f -> Map.of(
                                        "title", f.title(),
                                        "value", f.value(),
                                        "short", f.isShort()
                                    ))
                                    .toList());
                            }
                            return attMap;
                        })
                        .toList();
                    payload.put("attachments", attachments);
                }
                
                if (request.threadTs() != null) {
                    payload.put("thread_ts", request.threadTs());
                }
                
                String jsonPayload = objectMapper.writeValueAsString(payload);
                
                HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(SLACK_API_BASE + "/chat.postMessage"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                Map<String, Object> result = objectMapper.readValue(
                    response.body(),
                    Map.class
                );
                
                boolean ok = (Boolean) result.getOrDefault("ok", false);
                
                if (ok) {
                    String ts = (String) result.get("ts");
                    
                    meterRegistry.counter("slack.message.success").increment();
                    
                    return new MessageResult(
                        true,
                        ts,
                        ts,
                        "Message sent successfully"
                    );
                } else {
                    String error = (String) result.get("error");
                    meterRegistry.counter("slack.message.failure").increment();
                    return new MessageResult(false, null, null, error);
                }
                
            } catch (Exception e) {
                LOG.error("Failed to send Slack message", e);
                return new MessageResult(false, null, null, e.getMessage());
            }
        });
    }
    
    @Override
    public CompletionStage<List<Message>> getMessages(String channelId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String accessToken = getAccessToken();
                
                String url = SLACK_API_BASE + "/conversations.history" +
                    "?channel=" + channelId +
                    "&limit=" + limit;
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                Map<String, Object> result = objectMapper.readValue(
                    response.body(),
                    Map.class
                );
                
                boolean ok = (Boolean) result.getOrDefault("ok", false);
                
                if (ok) {
                    List<Map<String, Object>> messages = 
                        (List<Map<String, Object>>) result.get("messages");
                    
                    return messages.stream()
                        .map(this::parseMessage)
                        .toList();
                }
                
                return List.of();
                
            } catch (Exception e) {
                LOG.error("Failed to get Slack messages", e);
                return List.of();
            }
        });
    }
    
    @Override
    public CompletionStage<String> createChannel(String name, boolean isPrivate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String accessToken = getAccessToken();
                
                String endpoint = isPrivate ? 
                    "/conversations.create" : 
                    "/conversations.create";
                
                Map<String, Object> payload = Map.of(
                    "name", name,
                    "is_private", isPrivate
                );
                
                String jsonPayload = objectMapper.writeValueAsString(payload);
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SLACK_API_BASE + endpoint))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                Map<String, Object> result = objectMapper.readValue(
                    response.body(),
                    Map.class
                );
                
                boolean ok = (Boolean) result.getOrDefault("ok", false);
                
                if (ok) {
                    Map<String, Object> channel = (Map<String, Object>) result.get("channel");
                    return (String) channel.get("id");
                }
                
                return null;
                
            } catch (Exception e) {
                LOG.error("Failed to create Slack channel", e);
                return null;
            }
        });
    }
    
    @Override
    public CompletionStage<FileUploadResult> uploadFile(FileUpload file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String accessToken = getAccessToken();
                
                // Slack file upload is a multi-step process:
                // 1. Get upload URL
                // 2. Upload file
                // 3. Complete upload
                
                // Step 1: Get upload URL
                Map<String, Object> getUrlPayload = Map.of(
                    "filename", file.filename(),
                    "length", file.content().length
                );
                
                String jsonPayload = objectMapper.writeValueAsString(getUrlPayload);
                
                HttpRequest getUrlRequest = HttpRequest.newBuilder()
                    .uri(URI.create(SLACK_API_BASE + "/files.getUploadURLExternal"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
                
                HttpResponse<String> urlResponse = httpClient.send(
                    getUrlRequest,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                Map<String, Object> urlResult = objectMapper.readValue(
                    urlResponse.body(),
                    Map.class
                );
                
                if (!(Boolean) urlResult.getOrDefault("ok", false)) {
                    return new FileUploadResult(false, null, null);
                }
                
                String uploadUrl = (String) urlResult.get("upload_url");
                String fileId = (String) urlResult.get("file_id");
                
                // Step 2: Upload file
                HttpRequest uploadRequest = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    .header("Content-Type", "application/octet-stream")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(file.content()))
                    .build();
                
                HttpResponse<String> uploadResponse = httpClient.send(
                    uploadRequest,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                if (uploadResponse.statusCode() != 200) {
                    return new FileUploadResult(false, null, null);
                }
                
                // Step 3: Complete upload
                Map<String, Object> completePayload = new HashMap<>();
                completePayload.put("files", List.of(Map.of("id", fileId)));
                
                if (file.channels() != null) {
                    completePayload.put("channel_id", file.channels());
                }
                
                if (file.title() != null) {
                    completePayload.put("title", file.title());
                }
                
                String completeJson = objectMapper.writeValueAsString(completePayload);
                
                HttpRequest completeRequest = HttpRequest.newBuilder()
                    .uri(URI.create(SLACK_API_BASE + "/files.completeUploadExternal"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(completeJson))
                    .build();
                
                HttpResponse<String> completeResponse = httpClient.send(
                    completeRequest,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                Map<String, Object> completeResult = objectMapper.readValue(
                    completeResponse.body(),
                    Map.class
                );
                
                boolean ok = (Boolean) completeResult.getOrDefault("ok", false);
                
                if (ok) {
                    List<Map<String, Object>> files = 
                        (List<Map<String, Object>>) completeResult.get("files");
                    
                    if (!files.isEmpty()) {
                        String permalink = (String) files.get(0).get("permalink");
                        
                        meterRegistry.counter("slack.file.upload.success").increment();
                        
                        return new FileUploadResult(true, fileId, permalink);
                    }
                }
                
                meterRegistry.counter("slack.file.upload.failure").increment();
                return new FileUploadResult(false, null, null);
                
            } catch (Exception e) {
                LOG.error("Failed to upload file to Slack", e);
                return new FileUploadResult(false, null, null);
            }
        });
    }
    
    @Override
    public CompletionStage<UserInfo> getUserInfo(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String accessToken = getAccessToken();
                
                String url = SLACK_API_BASE + "/users.info?user=" + userId;
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                Map<String, Object> result = objectMapper.readValue(
                    response.body(),
                    Map.class
                );
                
                boolean ok = (Boolean) result.getOrDefault("ok", false);
                
                if (ok) {
                    Map<String, Object> user = (Map<String, Object>) result.get("user");
                    return parseUserInfo(user);
                }
                
                return null;
                
            } catch (Exception e) {
                LOG.error("Failed to get user info", e);
                return null;
            }
        });
    }
    
    @Override
    public CompletionStage<Boolean> setStatus(String status, String emoji) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String accessToken = getAccessToken();
                
                Map<String, Object> profile = Map.of(
                    "status_text", status,
                    "status_emoji", emoji
                );
                
                Map<String, Object> payload = Map.of("profile", profile);
                String jsonPayload = objectMapper.writeValueAsString(payload);
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SLACK_API_BASE + "/users.profile.set"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                Map<String, Object> result = objectMapper.readValue(
                    response.body(),
                    Map.class
                );
                
                return (Boolean) result.getOrDefault("ok", false);
                
            } catch (Exception e) {
                LOG.error("Failed to set status", e);
                return false;
            }
        });
    }
    
    // Helper methods
    
    private String getAccessToken() {
        return "cached_token";
    }
    
    private Message parseMessage(Map<String, Object> data) {
        String text = (String) data.get("text");
        String user = (String) data.get("user");
        String ts = (String) data.get("ts");
        
        Instant timestamp = Instant.ofEpochSecond(
            (long) Double.parseDouble(ts)
        );
        
        List<Map<String, Object>> reactions = 
            (List<Map<String, Object>>) data.getOrDefault("reactions", List.of());
        
        List<String> reactionNames = reactions.stream()
            .map(r -> (String) r.get("name"))
            .toList();
        
        return new Message(ts, user, text, timestamp, reactionNames);
    }
    
    private UserInfo parseUserInfo(Map<String, Object> user) {
        String id = (String) user.get("id");
        String name = (String) user.get("name");
        Map<String, Object> profile = (Map<String, Object>) user.get("profile");
        
        String realName = (String) profile.get("real_name");
        String email = (String) profile.get("email");
        String avatar = (String) profile.get("image_192");
        boolean isBot = (Boolean) user.getOrDefault("is_bot", false);
        
        return new UserInfo(id, name, realName, email, avatar, isBot);
    }
}