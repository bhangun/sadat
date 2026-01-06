package tech.kayys.wayang.websocket.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.websocket.dto.WebSocketSession;

/**
 * WebSocket session manager
 */
@ApplicationScoped
public class WebSocketSessionManager {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketSessionManager.class);

    // connectionId -> session
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // channel:resourceId -> Set<connectionId>
    private final Map<String, Set<String>> subscriptions = new ConcurrentHashMap<>();

    // connectionId -> Set<subscription keys>
    private final Map<String, Set<String>> connectionSubscriptions = new ConcurrentHashMap<>();

    public void registerSession(String connectionId, WebSocketSession session) {
        sessions.put(connectionId, session);
        connectionSubscriptions.put(connectionId, ConcurrentHashMap.newKeySet());
        LOG.info("Registered session for tenant: {}, user: {}",
                session.tenantId(), session.userId());
    }

    public void unregisterSession(String connectionId) {
        WebSocketSession session = sessions.remove(connectionId);
        if (session != null) {
            // Clean up subscriptions
            Set<String> subs = connectionSubscriptions.remove(connectionId);
            if (subs != null) {
                subs.forEach(key -> {
                    Set<String> connections = subscriptions.get(key);
                    if (connections != null) {
                        connections.remove(connectionId);
                        if (connections.isEmpty()) {
                            subscriptions.remove(key);
                        }
                    }
                });
            }
            LOG.info("Unregistered session: {}", connectionId);
        }
    }

    public WebSocketSession getSession(String connectionId) {
        return sessions.get(connectionId);
    }

    public void subscribe(String connectionId, String channel, String resourceId) {
        String key = channel + ":" + resourceId;
        subscriptions.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet())
                .add(connectionId);
        connectionSubscriptions.get(connectionId).add(key);
        LOG.debug("Subscription: {} -> {}", connectionId, key);
    }

    public void unsubscribe(String connectionId, String channel, String resourceId) {
        String key = channel + ":" + resourceId;
        Set<String> connections = subscriptions.get(key);
        if (connections != null) {
            connections.remove(connectionId);
        }
        Set<String> connSubs = connectionSubscriptions.get(connectionId);
        if (connSubs != null) {
            connSubs.remove(key);
        }
    }

    /**
     * Get all connection IDs subscribed to a channel/resource
     */
    public Set<String> getSubscribers(String channel, String resourceId) {
        String key = channel + ":" + resourceId;
        return subscriptions.getOrDefault(key, Set.of());
    }

    /**
     * Get all sessions for a tenant
     */
    public List<WebSocketSession> getTenantSessions(String tenantId) {
        return sessions.values().stream()
                .filter(s -> s.tenantId().equals(tenantId))
                .toList();
    }
}
