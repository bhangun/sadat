package tech.kayys.wayang.websocket;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import tech.kayys.wayang.collab.UserPresence;

/**
 * WorkspaceWebSocket - Real-time workspace updates
 */
@ServerEndpoint(value = "/ws/workspaces", encoders = { ResourceEventEncoder.class }, decoders = {
        ResourceEventDecoder.class }, configurator = WebSocketConfigurator.class)
@ApplicationScoped
public class WorkspaceWebSocket {

    // Tenant sessions: tenantId -> Set<Session>
    private static final Map<String, Set<Session>> tenantSessions = new ConcurrentHashMap<>();

    // Tenant presence: tenantId -> userId -> UserPresence
    private static final Map<String, Map<String, UserPresence>> tenantPresence = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        String tenantId = (String) session.getUserProperties().get("tenantId");
        String userId = (String) session.getUserProperties().get("userId");

        if (tenantId == null) {
            closeSession(session, "Missing authentication headers");
            return;
        }

        Log.debugf("User %s connected to workspace updates for tenant %s", userId, tenantId);

        tenantSessions.computeIfAbsent(tenantId, k -> ConcurrentHashMap.newKeySet())
                .add(session);

        // Track presence
        UserPresence presence = new UserPresence(
                userId,
                tenantId,
                (String) session.getUserProperties().getOrDefault("userName", "Anonymous"),
                Instant.now(),
                UserPresence.Status.ONLINE);

        tenantPresence.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>())
                .put(userId, presence);

        // Broadcast presence update
        broadcast(tenantId, new ResourceEvent(
                ResourceEventType.UPDATED,
                "presence",
                tenantId,
                userId,
                Map.of("type", "PRESENCE_JOINED", "user", presence)));

        // Send initial presence list
        sendPresenceList(session, tenantId);
    }

    @OnClose
    public void onClose(Session session) {
        String tenantId = (String) session.getUserProperties().get("tenantId");
        if (tenantId != null) {
            Set<Session> sessions = tenantSessions.get(tenantId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    tenantSessions.remove(tenantId);
                    tenantPresence.remove(tenantId);
                }
            }

            // Remove presence
            Map<String, UserPresence> presenceMap = tenantPresence.get(tenantId);
            String userId = (String) session.getUserProperties().get("userId");
            if (presenceMap != null && userId != null) {
                UserPresence presence = presenceMap.remove(userId);
                if (presence != null) {
                    broadcast(tenantId, new ResourceEvent(
                            ResourceEventType.UPDATED,
                            "presence",
                            tenantId,
                            userId,
                            Map.of("type", "PRESENCE_LEFT", "user", presence)));
                }
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        Log.errorf(throwable, "Workspace WebSocket error for session %s", session.getId());
    }

    public void broadcast(String tenantId, ResourceEvent event) {
        Set<Session> sessions = tenantSessions.get(tenantId);
        if (sessions != null) {
            sessions.forEach(session -> {
                if (session.isOpen()) {
                    session.getAsyncRemote().sendObject(event);
                }
            });
        }
    }

    private void sendPresenceList(Session session, String tenantId) {
        Map<String, UserPresence> presenceMap = tenantPresence.get(tenantId);
        if (presenceMap != null) {
            ResourceEvent event = new ResourceEvent(
                    ResourceEventType.UPDATED,
                    "presence",
                    tenantId,
                    "system",
                    Map.of("type", "PRESENCE_LIST", "users", presenceMap.values()));
            session.getAsyncRemote().sendObject(event);
        }
    }

    private void closeSession(Session session, String reason) {
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, reason));
        } catch (IOException e) {
            Log.error("Failed to close session", e);
        }
    }
}
