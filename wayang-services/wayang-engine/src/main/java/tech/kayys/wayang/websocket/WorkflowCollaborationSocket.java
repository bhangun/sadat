package tech.kayys.wayang.websocket;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import tech.kayys.wayang.utils.JsonUtils;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnOpen;
import jakarta.websocket.server.PathParam;
import tech.kayys.wayang.collab.CursorPosition;
import tech.kayys.wayang.schema.CollaborationEvent;
import tech.kayys.wayang.schema.CollaborationMessage;
import tech.kayys.wayang.schema.EventType;
import tech.kayys.wayang.schema.NodeMovePayload;
import tech.kayys.wayang.schema.NodeUpdatePayload;
import tech.kayys.wayang.schema.SelectionPayload;
import tech.kayys.wayang.schema.ConnectionPayload;
import tech.kayys.wayang.service.CollaborationService;

import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Workflow Collaboration WebSocket
 * Handles real-time multi-user editing
 */
@WebSocket(path = "/ws/workflows/{workflowId}")
public class WorkflowCollaborationSocket {

    private static final Logger LOG = Logger.getLogger(WorkflowCollaborationSocket.class);

    @Inject
    CollaborationService collaborationService;

    // Active sessions: workflowId -> Set<sessionId>
    private final Map<String, Map<String, WebSocketConnection>> activeSessions = new ConcurrentHashMap<>();

    /**
     * Client connected
     */
    @OnOpen
    public Uni<Void> onOpen(WebSocketConnection connection,
            @PathParam(value = "workflow-id") String workflowId) {

        String sessionId = connection.id();
        String userId = extractUserId(connection);
        String tenantId = extractTenantId(connection);

        LOG.infof("WS: User %s connected to workflow %s (session=%s, tenant=%s)",
                userId, workflowId, sessionId, tenantId);

        // Register session
        activeSessions
                .computeIfAbsent(workflowId, k -> new ConcurrentHashMap<>())
                .put(sessionId, connection);

        // Notify other users
        return collaborationService.userJoined(workflowId, userId, tenantId)
                .invoke(event -> broadcastToWorkflow(workflowId, event, sessionId))
                .replaceWithVoid();
    }

    /**
     * Client disconnected
     */
    @OnClose
    public Uni<Void> onClose(WebSocketConnection connection,
            @PathParam(value = "workflow-id") String workflowId) {

        String sessionId = connection.id();
        String userId = extractUserId(connection);
        String tenantId = extractTenantId(connection);

        LOG.infof("WS: User %s disconnected from workflow %s", userId, workflowId);

        // Unregister session
        Map<String, WebSocketConnection> sessions = activeSessions.get(workflowId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                activeSessions.remove(workflowId);
            }
        }

        // Release locks and notify
        return collaborationService.userLeft(workflowId, userId, tenantId)
                .invoke(event -> broadcastToWorkflow(workflowId, event, sessionId))
                .replaceWithVoid();
    }

    /**
     * Handle incoming messages
     */
    @OnTextMessage
    public Uni<Void> onMessage(String message,
            WebSocketConnection connection,
            @PathParam(value = "workflow-id") String workflowId) {

        String userId = extractUserId(connection);
        String tenantId = extractTenantId(connection);
        String sessionId = connection.id();

        CollaborationMessage msg = JsonUtils.fromJson(message, CollaborationMessage.class);

        LOG.debugf("WS: Received %s from user %s on workflow %s",
                msg.getType(), userId, workflowId);

        return switch (msg.getType()) {
            case CURSOR_MOVE -> handleCursorMove(workflowId, userId, msg, sessionId);
            case NODE_MOVE -> handleNodeMove(workflowId, userId, msg, tenantId, sessionId);
            case NODE_LOCK -> handleNodeLock(workflowId, userId, msg, tenantId, sessionId);
            case NODE_UNLOCK -> handleNodeUnlock(workflowId, userId, msg, tenantId, sessionId);
            case NODE_UPDATE -> handleNodeUpdate(workflowId, userId, msg, tenantId, sessionId);
            case CONNECTION_ADD -> handleConnectionAdd(workflowId, userId, msg, tenantId, sessionId);
            case CONNECTION_DELETE -> handleConnectionDelete(workflowId, userId, msg, tenantId, sessionId);
            case SELECTION_CHANGE -> handleSelectionChange(workflowId, userId, msg, sessionId);
            default -> Uni.createFrom().voidItem();
        };
    }

    /**
     * Handle cursor movement
     */
    private Uni<Void> handleCursorMove(String workflowId, String userId,
            CollaborationMessage msg, String excludeSession) {

        CursorPosition cursor = msg.getPayload(CursorPosition.class);
        cursor.setUserId(userId);
        cursor.setTimestamp(Instant.ofEpochMilli(System.currentTimeMillis()));

        // Broadcast to others immediately (no persistence)
        CollaborationEvent event = CollaborationEvent.builder()
                .type(EventType.CURSOR_MOVED)
                .userId(userId)
                .workflowId(workflowId)
                .payload(cursor)
                .build();

        broadcastToWorkflow(workflowId, event, excludeSession);
        return Uni.createFrom().voidItem();
    }

    /**
     * Handle node movement
     */
    private Uni<Void> handleNodeMove(String workflowId, String userId,
            CollaborationMessage msg, String tenantId,
            String excludeSession) {

        NodeMovePayload moveData = msg.getPayload(NodeMovePayload.class);

        return collaborationService.moveNode(
                workflowId, moveData.getNodeId(),
                moveData.getPosition(), userId, tenantId)
                .invoke(event -> broadcastToWorkflow(workflowId, event, excludeSession))
                .replaceWithVoid();
    }

    /**
     * Handle node lock
     */
    private Uni<Void> handleNodeLock(String workflowId, String userId,
            CollaborationMessage msg, String tenantId,
            String excludeSession) {

        String nodeId = msg.getPayload(String.class);

        return collaborationService.lockNode(workflowId, nodeId, userId, tenantId)
                .invoke(event -> broadcastToWorkflow(workflowId, event, excludeSession))
                .replaceWithVoid()
                .onFailure().recoverWithUni(error -> {
                    // Send error back to requesting user only
                    CollaborationEvent errorEvent = CollaborationEvent.builder()
                            .type(EventType.LOCK_FAILED)
                            .userId(userId)
                            .workflowId(workflowId)
                            .payload(Map.of("nodeId", nodeId, "error", error.getMessage()))
                            .build();

                    sendToSession(excludeSession, errorEvent);
                    return Uni.createFrom().voidItem();
                });
    }

    /**
     * Handle node unlock
     */
    private Uni<Void> handleNodeUnlock(String workflowId, String userId,
            CollaborationMessage msg, String tenantId,
            String excludeSession) {

        String nodeId = msg.getPayload(String.class);

        return collaborationService.unlockNode(workflowId, nodeId, userId, tenantId)
                .invoke(event -> broadcastToWorkflow(workflowId, event, excludeSession))
                .replaceWithVoid();
    }

    /**
     * Handle node update
     */
    private Uni<Void> handleNodeUpdate(String workflowId, String userId,
            CollaborationMessage msg, String tenantId,
            String excludeSession) {

        NodeUpdatePayload updateData = msg.getPayload(NodeUpdatePayload.class);

        return collaborationService.updateNode(
                workflowId, updateData.getNodeId(),
                updateData.getChanges(), userId, tenantId)
                .invoke(event -> broadcastToWorkflow(workflowId, event, excludeSession))
                .replaceWithVoid();
    }

    /**
     * Handle connection add
     */
    private Uni<Void> handleConnectionAdd(String workflowId, String userId,
            CollaborationMessage msg, String tenantId,
            String excludeSession) {

        ConnectionPayload connData = msg.getPayload(ConnectionPayload.class);

        return collaborationService.addConnection(workflowId, connData, userId, tenantId)
                .invoke(event -> broadcastToWorkflow(workflowId, event, excludeSession))
                .replaceWithVoid();
    }

    /**
     * Handle connection delete
     */
    private Uni<Void> handleConnectionDelete(String workflowId, String userId,
            CollaborationMessage msg, String tenantId,
            String excludeSession) {

        String connectionId = msg.getPayload(String.class);

        return collaborationService.deleteConnection(workflowId, connectionId, userId, tenantId)
                .invoke(event -> broadcastToWorkflow(workflowId, event, excludeSession))
                .replaceWithVoid();
    }

    /**
     * Handle selection change
     */
    private Uni<Void> handleSelectionChange(String workflowId, String userId,
            CollaborationMessage msg, String excludeSession) {

        SelectionPayload selection = msg.getPayload(SelectionPayload.class);
        selection.setUserId(userId);

        CollaborationEvent event = CollaborationEvent.builder()
                .type(EventType.SELECTION_CHANGED)
                .userId(userId)
                .workflowId(workflowId)
                .payload(selection)
                .build();

        broadcastToWorkflow(workflowId, event, excludeSession);
        return Uni.createFrom().voidItem();
    }

    /**
     * Broadcast event to all users in workflow except sender
     */
    private void broadcastToWorkflow(String workflowId, CollaborationEvent event,
            String excludeSessionId) {

        Map<String, WebSocketConnection> sessions = activeSessions.get(workflowId);
        if (sessions == null)
            return;

        String eventJson = JsonUtils.toJson(event);

        sessions.forEach((sessionId, connection) -> {
            if (!sessionId.equals(excludeSessionId)) {
                connection.sendText(eventJson);
            }
        });
    }

    /**
     * Send event to specific session
     */
    private void sendToSession(String sessionId, CollaborationEvent event) {
        activeSessions.values().stream()
                .flatMap(sessions -> sessions.entrySet().stream())
                .filter(entry -> entry.getKey().equals(sessionId))
                .findFirst()
                .ifPresent(entry -> {
                    String eventJson = JsonUtils.toJson(event);
                    entry.getValue().sendText(eventJson);
                });
    }

    private String extractUserId(WebSocketConnection connection) {
        return connection.handshakeRequest()
                .header("X-User-ID")
                .orElse("anonymous");
    }

    private String extractTenantId(WebSocketConnection connection) {
        return connection.handshakeRequest()
                .header("X-Tenant-ID")
                .orElse("default");
    }
}
