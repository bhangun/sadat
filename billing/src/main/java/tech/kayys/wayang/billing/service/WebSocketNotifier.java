package tech.kayys.wayang.billing.service;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WebSocketNotifier {
    
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketNotifier.class);
    
    private final Map<String, Set<jakarta.websocket.Session>> tenantSessions = 
        new java.util.concurrent.ConcurrentHashMap<>();
    
    public Uni<Void> notifyOrganization(
            String tenantId,
            String eventType,
            Map<String, Object> payload) {
        LOG.debug("Notifying tenant: {} of event: {}", tenantId, eventType);
        Set<jakarta.websocket.Session> sessions = tenantSessions.get(tenantId);
        if (sessions == null || sessions.isEmpty()) {
            return Uni.createFrom().voidItem();
        }
        
        String message = buildMessage(eventType, payload);
        
        sessions.forEach(session -> {
            if (session.isOpen()) {
                session.getAsyncRemote().sendText(message);
            }
        });
        
        return Uni.createFrom().voidItem();
    }
    
    private String buildMessage(String eventType, Map<String, Object> payload) {
        return String.format("{\"type\":\"%s\",\"payload\":%s}", 
            eventType, 
            new com.fasterxml.jackson.databind.ObjectMapper()
                .valueToTree(payload).toString()
        );
    }
}

