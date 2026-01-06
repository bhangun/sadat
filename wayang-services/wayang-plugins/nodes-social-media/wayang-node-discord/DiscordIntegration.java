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
 * Discord API integration.
 * 
 * Supports:
 * - Bot token authentication
 * - Send messages with embeds
 * - Channel management
 * - Webhook support
 * - Reactions
 */
@ApplicationScoped
public class DiscordIntegration implements MessagingIntegration {
    
    private static final Logger LOG = LoggerFactory.getLogger(DiscordIntegration.class);
    private static final String DISCORD_API_BASE = "https://discord.com/api/v10";
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final String botToken;
    
    @Inject
    public DiscordIntegration(
        ObjectMapper objectMapper,
        MeterRegistry meterRegistry,
        @ConfigProperty(name = "wayang.integration.discord.bot-token") String botToken
    ) {
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.botToken = botToken;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }
    
    @Override
    public CompletionStage<MessageResult> sendMessage(MessageRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("content", request.text());
                
                // Convert blocks to Discord embeds
                if (request.blocks() != null && !request.blocks().isEmpty()) {
                    List<Map<String, Object>> embeds = request.blocks().stream()
                        .map(block -> {
                            Map<String, Object> embed = new HashMap<>(block.content());
                            return embed;
                        })
                        .toList();
                    payload.put("embeds", embeds);
                }
                
                String jsonPayload = objectMapper.writeValueAsString(payload);
                
                HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(DISCORD_API_BASE + "/channels/" + 
                        request.channel() + "/messages"))
                    .header("Authorization", "Bot " + botToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                if (response.statusCode() == 200) {
                    Map<String, Object> result = objectMapper.readValue(
                        response.body(),
                        Map.class
                    );
                    
                    String messageId = (String) result.get("id");
                    
                    meterRegistry.counter("discord.message.success").increment();
                    
                    return new MessageResult(
                        true,
                        messageId,
                        messageId,
                        "Message sent successfully"
                    );
                } else {
                    meterRegistry.counter("discord.message.failure").increment();
                    return new MessageResult(false, null, null, "Failed to send message");
                }
                
            } catch (Exception e) {
                LOG.error("Failed to send Discord message", e);
                return new MessageResult(false, null, null, e.getMessage());
            }
        });
    }
    
    @Override
    public CompletionStage<List<Message>> getMessages(String channelId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = DISCORD_API_BASE + "/channels/" + channelId + 
                    "/messages?limit=" + limit;
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bot " + botToken)
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                if (response.statusCode() == 200) {
                    List<Map<String, Object>> messages = objectMapper.readValue(
                        response.body(),
                        List.class
                    );
                    
                    return messages.stream()
                        .map(this::parseMessage)
                        .toList();
                }
                
                return List.of();
                
            } catch (Exception e) {
                LOG.error("Failed to get Discord messages", e);
                return List.of();
            }
        });
    }
    
    @Override
    public CompletionStage<String> createChannel(String name, boolean isPrivate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Note: Requires guild ID - would come from context
                String guildId = "guild_id";
                
                Map<String, Object> payload = Map.of(
                    "name", name,
                    "type", isPrivate ? 0 : 0 // 0 = text channel
                );
                
                String jsonPayload = objectMapper.writeValueAsString(payload);
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DISCORD_API_BASE + "/guilds/" + guildId + "/channels"))
                    .header("Authorization", "Bot " + botToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                if (response.statusCode() == 201) {
                    Map<String, Object> result = objectMapper.readValue(
                        response.body(),
                        Map.class
                    );
                    return (String) result.get("id");
                }
                
                return null;
                
            } catch (Exception e) {
                LOG.error("Failed to create Discord channel", e);
                return null;
            }
        });
    }
    
    @Override
    public CompletionStage<FileUploadResult> uploadFile(FileUpload file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Discord file upload via multipart/form-data
                // Simplified version - would need proper multipart implementation
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DISCORD_API_BASE + "/channels/" + 
                        file.channels() + "/messages"))
                    .header("Authorization", "Bot " + botToken)
                    .header("Content-Type", "multipart/form-data")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(file.content()))
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                if (response.statusCode() == 200) {
                    Map<String, Object> result = objectMapper.readValue(
                        response.body(),
                        Map.class
                    );
                    
                    List<Map<String, Object>> attachments = 
                        (List<Map<String, Object>>) result.get("attachments");
                    
                    if (!attachments.isEmpty()) {
                        String fileId = (String) attachments.get(0).get("id");
                        String url = (String) attachments.get(0).get("url");
                        
                        return new FileUploadResult(true, fileId, url);
                    }
                }
                
                return new FileUploadResult(false, null, null);
                
            } catch (Exception e) {
                LOG.error("Failed to upload file to Discord", e);
                return new FileUploadResult(false, null, null);
            }
        });
    }
    
    @Override
    public CompletionStage<UserInfo> getUserInfo(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DISCORD_API_BASE + "/users/" + userId))
                    .header("Authorization", "Bot " + botToken)
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                if (response.statusCode() == 200) {
                    Map<String, Object> user = objectMapper.readValue(
                        response.body(),
                        Map.class
                    );
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
        // Discord status is set via Gateway WebSocket, not REST API
        LOG.warn("Discord status setting requires WebSocket Gateway connection");
        return CompletableFuture.completedFuture(false);
    }
    
    // Helper methods
    
    private Message parseMessage(Map<String, Object> data) {
        String id = (String) data.get("id");
        String content = (String) data.get("content");
        Map<String, Object> author = (Map<String, Object>) data.get("author");
        String authorId = (String) author.get("id");
        String timestamp = (String) data.get("timestamp");
        
        Instant ts = Instant.parse(timestamp);
        
        List<Map<String, Object>> reactions = 
            (List<Map<String, Object>>) data.getOrDefault("reactions", List.of());
        
        List<String> reactionEmojis = reactions.stream()
            .map(r -> {
                Map<String, Object> emoji = (Map<String, Object>) r.get("emoji");
                return (String) emoji.get("name");
            })
            .toList();
        
        return new Message(id, authorId, content, ts, reactionEmojis);
    }
    
    private UserInfo parseUserInfo(Map<String, Object> user) {
        String id = (String) user.get("id");
        String username = (String) user.get("username");
        String discriminator = (String) user.get("discriminator");
        String avatar = (String) user.get("avatar");
        boolean isBot = (Boolean) user.getOrDefault("bot", false);
        
        String name = username + "#" + discriminator;
        String avatarUrl = avatar != null ?
            "https://cdn.discordapp.com/avatars/" + id + "/" + avatar + ".png" :
            null;
        
        return new UserInfo(id, username, name, null, avatarUrl, isBot);
    }
}