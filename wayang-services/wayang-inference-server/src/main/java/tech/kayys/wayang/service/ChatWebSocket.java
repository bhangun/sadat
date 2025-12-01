package tech.kayys.wayang.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import tech.kayys.wayang.engine.LlamaConfig.SamplingConfig;
import tech.kayys.wayang.model.ChatMessage;
import tech.kayys.wayang.plugin.ModelManager;

public class ChatWebSocket {
    private static final Logger log = Logger.getLogger(ChatWebSocket.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();
    
    @Inject
    ModelManager modelManager;
    
    @OnOpen
    public void onOpen(Session session) {
        sessions.put(session.getId(), session);
        log.infof("WebSocket connected: %s", session.getId());
        
        sendMessage(session, Map.of(
            "type", "connected",
            "session_id", session.getId()
        ));
    }
    
    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            Map<String, Object> request = mapper.readValue(message, Map.class);
            String type = (String) request.get("type");
            
            if ("chat".equals(type)) {
                handleChatRequest(request, session);
            } else if ("stop".equals(type)) {
                handleStopRequest(session);
            }
            
        } catch (Exception e) {
            log.error("Error processing WebSocket message", e);
            sendError(session, "Invalid request: " + e.getMessage());
        }
    }
    
    @OnClose
    public void onClose(Session session) {
        sessions.remove(session.getId());
        log.infof("WebSocket disconnected: %s", session.getId());
    }
    
    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("WebSocket error", throwable);
        sendError(session, "Error: " + throwable.getMessage());
    }
    
    private void handleChatRequest(Map<String, Object> request, Session session) {
        try {
            List<Map<String, String>> messagesData = 
                (List<Map<String, String>>) request.get("messages");
            
            List<ChatMessage> messages = messagesData.stream()
                .map(m -> new ChatMessage(m.get("role"), m.get("content")))
                .toList();
            
            Integer maxTokens = (Integer) request.getOrDefault("max_tokens", 512);
            Float temperature = ((Number) request.getOrDefault("temperature", 0.8)).floatValue();
            
            SamplingConfig config = SamplingConfig.builder()
                .temperature(temperature)
                .build();
            
            // Send start message
            sendMessage(session, Map.of("type", "start"));
            
            // Generate with streaming
            modelManager.getActiveModel().chat(messages, config, maxTokens, 
                piece -> sendMessage(session, Map.of(
                    "type", "token",
                    "content", piece
                ))
            );
            
            // Send done message
            sendMessage(session, Map.of("type", "done"));
            
        } catch (Exception e) {
            log.error("Chat generation failed", e);
            sendError(session, "Generation failed: " + e.getMessage());
        }
    }
    
    private void handleStopRequest(Session session) {
        // Implement stop logic
        sendMessage(session, Map.of("type", "stopped"));
    }
    
    private void sendMessage(Session session, Map<String, Object> data) {
        try {
            if (session.isOpen()) {
                String json = mapper.writeValueAsString(data);
                session.getAsyncRemote().sendText(json);
            }
        } catch (Exception e) {
            log.error("Error sending WebSocket message", e);
        }
    }
    
    private void sendError(Session session, String error) {
        sendMessage(session, Map.of(
            "type", "error",
            "error", error
        ));
    }
}
