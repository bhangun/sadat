package tech.kayys.wayang.websocket;

import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.schema.CollaborationEvent;
import tech.kayys.wayang.schema.CollaborationMessage;
import tech.kayys.wayang.schema.EventType;
import tech.kayys.wayang.utils.JsonUtils;

/**
 * CollaborationConnection - Represents an active WebSocket connection
 */
@WebSocketClient(path = "/ws/workflows/{workflowId}")
public class CollaborationConnection {

    private static final Logger LOG = Logger.getLogger(CollaborationConnection.class);

    private final String workflowId;
    private final String userId;
    private final String tenantId;
    private final CollaborationHandler handler;

    private WebSocketConnection connection;
    private boolean connected = false;

    public CollaborationConnection(String workflowId, String userId,
            String tenantId, CollaborationHandler handler) {
        this.workflowId = workflowId;
        this.userId = userId;
        this.tenantId = tenantId;
        this.handler = handler;
    }

    @OnOpen
    public void onOpen(WebSocketConnection connection) {
        this.connection = connection;
        this.connected = true;
        LOG.infof("WS connected: workflow=%s, user=%s", workflowId, userId);
        handler.onConnected(workflowId);
    }

    @OnTextMessage
    public void onMessage(String message) {
        LOG.debugf("WS message received: workflow=%s", workflowId);

        try {
            CollaborationEvent event = JsonUtils.fromJson(message,
                    CollaborationEvent.class);

            // Route event to handler
            switch (event.getType()) {
                case USER_JOINED -> handler.onUserJoined(event);
                case USER_LEFT -> handler.onUserLeft(event);
                case CURSOR_MOVED -> handler.onCursorMoved(event);
                case NODE_MOVED -> handler.onNodeMoved(event);
                case NODE_LOCKED -> handler.onNodeLocked(event);
                case NODE_UNLOCKED -> handler.onNodeUnlocked(event);
                case NODE_UPDATED -> handler.onNodeUpdated(event);
                case CONNECTION_ADDED -> handler.onConnectionAdded(event);
                case CONNECTION_DELETED -> handler.onConnectionDeleted(event);
                case SELECTION_CHANGED -> handler.onSelectionChanged(event);
                case LOCK_FAILED -> handler.onLockFailed(event);
                case ERROR -> handler.onError(event);
                default -> LOG.warnf("Unknown event type: %s", event.getType());
            }
        } catch (Exception e) {
            LOG.errorf(e, "Failed to process WS message: %s", message);
            handler.onError(buildErrorEvent(e));
        }
    }

    @OnClose
    public void onClose() {
        this.connected = false;
        LOG.infof("WS disconnected: workflow=%s", workflowId);
        handler.onDisconnected(workflowId);
    }

    @OnError
    public void onError(Throwable error) {
        LOG.errorf(error, "WS error: workflow=%s", workflowId);
        handler.onError(buildErrorEvent(error));
    }

    /**
     * Connect to WebSocket
     */
    public Uni<Void> connect() {
        // Connection is handled by @OnOpen
        return Uni.createFrom().voidItem();
    }

    /**
     * Send message
     */
    public Uni<Void> send(CollaborationMessage message) {
        if (!connected || connection == null) {
            return Uni.createFrom().failure(
                    new IllegalStateException("Not connected"));
        }

        String json = JsonUtils.toJson(message);
        connection.sendText(json);
        return Uni.createFrom().voidItem();
    }

    /**
     * Close connection
     */
    public Uni<Void> close() {
        if (connection != null) {
            connection.close();
        }
        return Uni.createFrom().voidItem();
    }

    private CollaborationEvent buildErrorEvent(Throwable error) {
        return CollaborationEvent.builder()
                .type(EventType.ERROR)
                .userId(userId)
                .workflowId(workflowId)
                .payload(Map.of("error", error.getMessage()))
                .build();
    }
}
