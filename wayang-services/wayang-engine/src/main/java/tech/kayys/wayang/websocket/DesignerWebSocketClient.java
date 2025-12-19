package tech.kayys.wayang.websocket;

import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.schema.CollaborationMessage;
import tech.kayys.wayang.schema.MessageType;
import tech.kayys.wayang.schema.PointDTO;

import org.jboss.logging.Logger;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * WebSocket Client for Workflow Collaboration
 * Connects Designer Service to Workflow Service WebSocket
 */
@ApplicationScoped
public class DesignerWebSocketClient {

    private static final Logger LOG = Logger.getLogger(DesignerWebSocketClient.class);

    // Active connections: workflowId -> WebSocketConnection
    private final Map<String, CollaborationConnection> activeConnections = new ConcurrentHashMap<>();

    @Inject
    WebSocketConnectionFactory connectionFactory;

    /**
     * Connect to workflow collaboration WebSocket
     */
    public Uni<Void> connect(String workflowId, String userId, String tenantId,
            CollaborationHandler handler) {

        LOG.infof("Designer: Connecting to collaboration WS for workflow %s", workflowId);

        return Uni.createFrom().item(() -> {
            String wsUrl = buildWebSocketUrl(workflowId);

            CollaborationConnection connection = connectionFactory.create(
                    wsUrl, userId, tenantId, handler);

            activeConnections.put(workflowId, connection);

            return connection;
        })
                .flatMap(CollaborationConnection::connect)
                .invoke(v -> LOG.infof("Designer: Connected to WS for workflow %s", workflowId));
    }

    /**
     * Disconnect from workflow
     */
    public Uni<Void> disconnect(String workflowId) {
        LOG.infof("Designer: Disconnecting from workflow %s", workflowId);

        CollaborationConnection connection = activeConnections.remove(workflowId);
        if (connection != null) {
            return connection.close();
        }
        return Uni.createFrom().voidItem();
    }

    /**
     * Send cursor position
     */
    public Uni<Void> sendCursorPosition(String workflowId, double x, double y) {
        return sendMessage(workflowId, MessageType.CURSOR_MOVE,
                Map.of("x", x, "y", y));
    }

    /**
     * Send node move
     */
    public Uni<Void> sendNodeMove(String workflowId, String nodeId, PointDTO position) {
        return sendMessage(workflowId, MessageType.NODE_MOVE,
                Map.of("nodeId", nodeId, "position", position));
    }

    /**
     * Send node lock request
     */
    public Uni<Void> sendNodeLock(String workflowId, String nodeId) {
        return sendMessage(workflowId, MessageType.NODE_LOCK, nodeId);
    }

    /**
     * Send node unlock request
     */
    public Uni<Void> sendNodeUnlock(String workflowId, String nodeId) {
        return sendMessage(workflowId, MessageType.NODE_UNLOCK, nodeId);
    }

    /**
     * Send node update
     */
    public Uni<Void> sendNodeUpdate(String workflowId, String nodeId,
            Map<String, Object> changes) {
        return sendMessage(workflowId, MessageType.NODE_UPDATE,
                Map.of("nodeId", nodeId, "changes", changes));
    }

    /**
     * Send connection add
     */
    public Uni<Void> sendConnectionAdd(String workflowId, ConnectionPayload connection) {
        return sendMessage(workflowId, MessageType.CONNECTION_ADD, connection);
    }

    /**
     * Send generic message
     */
    private Uni<Void> sendMessage(String workflowId, MessageType type, Object payload) {
        CollaborationConnection connection = activeConnections.get(workflowId);
        if (connection == null) {
            return Uni.createFrom().failure(
                    new IllegalStateException("Not connected to workflow: " + workflowId));
        }

        CollaborationMessage message = new CollaborationMessage();
        message.setType(type);
        message.setPayload(payload instanceof Map ? (Map<String, Object>) payload : Map.of("data", payload));

        return connection.send(message);
    }

    /**
     * Build WebSocket URL
     */
    private String buildWebSocketUrl(String workflowId) {
        String baseUrl = System.getenv()
                .getOrDefault("WORKFLOW_SERVICE_WS_URL", "ws://localhost:8081");
        return String.format("%s/ws/workflows/%s", baseUrl, workflowId);
    }
}