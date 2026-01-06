package tech.kayys.wayang.websocket.service;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

/**
 * WebSocket endpoint for real-time workflow updates
 */
@ServerEndpoint("/api/v1/control-plane/ws/{tenantId}")
public class ControlPlaneWebSocket {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ControlPlaneWebSocket.class);

    private static final Map<String, Set<jakarta.websocket.Session>> tenantSessions = new java.util.concurrent.ConcurrentHashMap<>();

    @jakarta.websocket.OnOpen
    public void onOpen(
            jakarta.websocket.Session session,
            @PathParam("tenantId") String tenantId) {

        LOG.info("WebSocket connection opened for tenant: {}", tenantId);

        tenantSessions.computeIfAbsent(tenantId, k -> java.util.concurrent.ConcurrentHashMap.newKeySet())
                .add(session);
    }

    @jakarta.websocket.OnClose
    public void onClose(
            jakarta.websocket.Session session,
            @PathParam("tenantId") String tenantId) {

        LOG.info("WebSocket connection closed for tenant: {}", tenantId);

        Set<jakarta.websocket.Session> sessions = tenantSessions.get(tenantId);
        if (sessions != null) {
            sessions.remove(session);
        }
    }

    @jakarta.websocket.OnMessage
    public void onMessage(String message, jakarta.websocket.Session session) {
        LOG.debug("Received WebSocket message: {}", message);
        // Handle commands from client
    }

    /**
     * Broadcast update to all tenant sessions
     */
    public static void broadcastUpdate(String tenantId, String message) {
        Set<jakarta.websocket.Session> sessions = tenantSessions.get(tenantId);
        if (sessions != null) {
            sessions.forEach(session -> {
                if (session.isOpen()) {
                    session.getAsyncRemote().sendText(message);
                }
            });
        }
    }
}