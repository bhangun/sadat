package tech.kayys.wayang.collab;

import io.quarkus.logging.Log;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import tech.kayys.wayang.service.LockService;
import tech.kayys.wayang.websocket.WebSocketConfigurator;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CollaborationWebSocket - Real-time collaborative editing
 * 
 * Features:
 * - Multi-user cursor tracking
 * - Real-time workflow updates
 * - Presence awareness
 * - Operational Transform for concurrent edits
 */
@ServerEndpoint(value = "/ws/collaborate/{workflowId}", encoders = { CollaborationMessageEncoder.class }, decoders = {
        CollaborationMessageDecoder.class }, configurator = WebSocketConfigurator.class)
@ApplicationScoped
public class CollaborationWebSocket {

    // Session management: workflowId -> Set<Session>
    private static final Map<UUID, Set<Session>> workflowSessions = new ConcurrentHashMap<>();

    // User presence: workflowId -> userId -> UserPresence
    private static final Map<UUID, Map<String, UserPresence>> workflowPresence = new ConcurrentHashMap<>();

    // Cursor positions: workflowId -> userId -> CursorPosition
    private static final Map<UUID, Map<String, CursorPosition>> workflowCursors = new ConcurrentHashMap<>();

    @Inject
    LockService lockService;

    @Inject
    SecurityIdentity securityIdentity;

    @OnOpen
    public void onOpen(Session session, @PathParam("workflowId") String workflowIdStr) {
        try {
            UUID workflowId = UUID.fromString(workflowIdStr);
            String userId = getUserId(session);
            String tenantId = getTenantId(session);

            Log.infof("User %s joining collaboration on workflow %s", userId, workflowId);

            // Add session to workflow sessions
            workflowSessions.computeIfAbsent(workflowId, k -> ConcurrentHashMap.newKeySet())
                    .add(session);

            // Register user presence
            UserPresence presence = new UserPresence(
                    userId,
                    tenantId,
                    getUserName(session),
                    Instant.now(),
                    UserPresence.Status.ONLINE);

            workflowPresence.computeIfAbsent(workflowId, k -> new ConcurrentHashMap<>())
                    .put(userId, presence);

            // Initialize cursor position
            workflowCursors.computeIfAbsent(workflowId, k -> new ConcurrentHashMap<>())
                    .put(userId, new CursorPosition(userId, 0, 0));

            // Notify others about new user
            broadcastPresenceUpdate(workflowId, presence, PresenceEvent.JOINED);

            // Send current presence list to new user
            sendPresenceList(session, workflowId);

        } catch (Exception e) {
            Log.errorf(e, "Error in WebSocket onOpen");
            closeSession(session, "Failed to join collaboration");
        }
    }

    @OnMessage
    public void onMessage(Session session, CollaborationMessage message,
            @PathParam("workflowId") String workflowIdStr) {
        try {
            UUID workflowId = UUID.fromString(workflowIdStr);
            String userId = getUserId(session);

            message.setSenderId(userId);
            message.setTimestamp(Instant.now());

            switch (message.getType()) {
                case CURSOR_MOVE -> handleCursorMove(session, workflowId, message);
                case WORKFLOW_UPDATE -> handleWorkflowUpdate(session, workflowId, message);
                case NODE_SELECT -> handleNodeSelect(session, workflowId, message);
                case TYPING_START -> handleTypingIndicator(session, workflowId, message, true);
                case TYPING_STOP -> handleTypingIndicator(session, workflowId, message, false);
                case LOCK_REQUEST -> handleLockRequest(session, workflowId, message);
                case UNLOCK_REQUEST -> handleUnlockRequest(session, workflowId, message);
                case PING -> handlePing(session, message);
                default -> Log.warnf("Unknown message type: %s", message.getType());
            }

        } catch (Exception e) {
            Log.errorf(e, "Error handling WebSocket message");
            sendError(session, "Failed to process message: " + e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("workflowId") String workflowIdStr,
            CloseReason closeReason) {
        try {
            UUID workflowId = UUID.fromString(workflowIdStr);
            String userId = getUserId(session);

            Log.infof("User %s leaving collaboration on workflow %s: %s",
                    userId, workflowId, closeReason);

            // Remove session
            Set<Session> sessions = workflowSessions.get(workflowId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    workflowSessions.remove(workflowId);
                    workflowPresence.remove(workflowId);
                    workflowCursors.remove(workflowId);
                }
            }

            // Update presence
            Map<String, UserPresence> presence = workflowPresence.get(workflowId);
            if (presence != null) {
                UserPresence userPresence = presence.remove(userId);
                if (userPresence != null) {
                    broadcastPresenceUpdate(workflowId, userPresence, PresenceEvent.LEFT);
                }
            }

            // Remove cursor
            Map<String, CursorPosition> cursors = workflowCursors.get(workflowId);
            if (cursors != null) {
                cursors.remove(userId);
            }

        } catch (Exception e) {
            Log.errorf(e, "Error in WebSocket onClose");
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable,
            @PathParam("workflowId") String workflowIdStr) {
        Log.errorf(throwable, "WebSocket error for workflow %s", workflowIdStr);
        closeSession(session, "Internal error occurred");
    }

    /**
     * Handle cursor movement
     */
    private void handleCursorMove(Session session, UUID workflowId, CollaborationMessage message) {
        String userId = message.getSenderId();
        CursorPosition cursor = message.getCursor();

        if (cursor != null) {
            cursor.setUserId(userId);
            cursor.setTimestamp(Instant.now());

            Map<String, CursorPosition> cursors = workflowCursors.get(workflowId);
            if (cursors != null) {
                cursors.put(userId, cursor);
            }

            // Broadcast to others (excluding sender)
            broadcastToOthers(workflowId, session, message);
        }
    }

    /**
     * Handle workflow update (node add/delete/move, connection changes)
     */
    private void handleWorkflowUpdate(Session session, UUID workflowId, CollaborationMessage message) {
        // Apply operational transform if needed
        WorkflowOperation operation = message.getOperation();

        if (operation != null) {
            // Transform operation against concurrent operations
            WorkflowOperation transformed = transformOperation(workflowId, operation);
            message.setOperation(transformed);
        }

        // Broadcast to all (including sender for confirmation)
        broadcastToAll(workflowId, message);
    }

    /**
     * Handle node selection
     */
    private void handleNodeSelect(Session session, UUID workflowId, CollaborationMessage message) {
        // Track which nodes are being edited by which users
        broadcastToOthers(workflowId, session, message);
    }

    /**
     * Handle typing indicator
     */
    private void handleTypingIndicator(Session session, UUID workflowId,
            CollaborationMessage message, boolean isTyping) {
        String userId = message.getSenderId();

        Map<String, UserPresence> presence = workflowPresence.get(workflowId);
        if (presence != null && presence.containsKey(userId)) {
            UserPresence userPresence = presence.get(userId);
            userPresence.setTyping(isTyping);
            userPresence.setTypingAt(isTyping ? Instant.now() : null);

            broadcastToOthers(workflowId, session, message);
        }
    }

    /**
     * Handle lock request
     */
    private void handleLockRequest(Session session, UUID workflowId, CollaborationMessage message) {
        String userId = message.getSenderId();
        String sessionId = session.getId();

        lockService.acquireLock(workflowId, sessionId)
                .subscribe().with(
                        lock -> {
                            CollaborationMessage response = new CollaborationMessage();
                            response.setType(MessageType.LOCK_ACQUIRED);
                            response.setLockId(lock.id.toString());
                            sendMessage(session, response);

                            // Notify others
                            CollaborationMessage notification = new CollaborationMessage();
                            notification.setType(MessageType.LOCK_NOTIFICATION);
                            notification.setSenderId(userId);
                            notification.setLockId(lock.id.toString());
                            broadcastToOthers(workflowId, session, notification);
                        },
                        error -> {
                            sendError(session, "Failed to acquire lock: " + error.getMessage());
                        });
    }

    /**
     * Handle unlock request
     */
    private void handleUnlockRequest(Session session, UUID workflowId, CollaborationMessage message) {
        String lockIdStr = message.getLockId();
        if (lockIdStr != null) {
            UUID lockId = UUID.fromString(lockIdStr);

            lockService.releaseLock(lockId)
                    .subscribe().with(
                            v -> {
                                CollaborationMessage response = new CollaborationMessage();
                                response.setType(MessageType.LOCK_RELEASED);
                                sendMessage(session, response);

                                // Notify others
                                broadcastToOthers(workflowId, session, response);
                            },
                            error -> {
                                sendError(session, "Failed to release lock: " + error.getMessage());
                            });
        }
    }

    /**
     * Handle ping (keep-alive)
     */
    private void handlePing(Session session, CollaborationMessage message) {
        CollaborationMessage pong = new CollaborationMessage();
        pong.setType(MessageType.PONG);
        pong.setTimestamp(Instant.now());
        sendMessage(session, pong);
    }

    /**
     * Operational Transform - resolve concurrent operations
     */
    private WorkflowOperation transformOperation(UUID workflowId, WorkflowOperation operation) {
        // Simplified OT - in production, use a proper OT library
        // This is a placeholder for demonstration
        return operation;
    }

    /**
     * Broadcast presence update
     */
    private void broadcastPresenceUpdate(UUID workflowId, UserPresence presence, PresenceEvent event) {
        CollaborationMessage message = new CollaborationMessage();
        message.setType(MessageType.PRESENCE_UPDATE);
        message.setPresence(presence);
        message.setPresenceEvent(event);
        message.setTimestamp(Instant.now());

        broadcastToAll(workflowId, message);
    }

    /**
     * Send presence list to new user
     */
    private void sendPresenceList(Session session, UUID workflowId) {
        Map<String, UserPresence> presence = workflowPresence.get(workflowId);
        if (presence != null) {
            CollaborationMessage message = new CollaborationMessage();
            message.setType(MessageType.PRESENCE_LIST);
            message.setPresenceList(new ArrayList<>(presence.values()));
            message.setTimestamp(Instant.now());

            sendMessage(session, message);
        }
    }

    /**
     * Broadcast to all sessions in workflow
     */
    private void broadcastToAll(UUID workflowId, CollaborationMessage message) {
        Set<Session> sessions = workflowSessions.get(workflowId);
        if (sessions != null) {
            for (Session session : sessions) {
                sendMessage(session, message);
            }
        }
    }

    /**
     * Broadcast to all except sender
     */
    private void broadcastToOthers(UUID workflowId, Session senderSession, CollaborationMessage message) {
        Set<Session> sessions = workflowSessions.get(workflowId);
        if (sessions != null) {
            for (Session session : sessions) {
                if (!session.getId().equals(senderSession.getId())) {
                    sendMessage(session, message);
                }
            }
        }
    }

    /**
     * Send message to specific session
     */
    private void sendMessage(Session session, CollaborationMessage message) {
        if (session.isOpen()) {
            try {
                session.getBasicRemote().sendObject(message);
            } catch (Exception e) {
                Log.errorf(e, "Failed to send message to session %s", session.getId());
            }
        }
    }

    /**
     * Send error message
     */
    private void sendError(Session session, String errorMessage) {
        CollaborationMessage message = new CollaborationMessage();
        message.setType(MessageType.ERROR);
        message.setError(errorMessage);
        message.setTimestamp(Instant.now());
        sendMessage(session, message);
    }

    /**
     * Close session with reason
     */
    private void closeSession(Session session, String reason) {
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, reason));
        } catch (IOException e) {
            Log.errorf(e, "Failed to close session");
        }
    }

    private String getUserId(Session session) {
        return (String) session.getUserProperties().get("userId");
    }

    private String getTenantId(Session session) {
        return (String) session.getUserProperties().get("tenantId");
    }

    private String getUserName(Session session) {
        return (String) session.getUserProperties().getOrDefault("userName", "Anonymous");
    }
}