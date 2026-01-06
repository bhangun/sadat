package tech.kayys.wayang.websocket.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.websocket.dto.AgentEvent;
import tech.kayys.wayang.websocket.dto.WebSocketSession;
import tech.kayys.wayang.websocket.dto.WorkflowEvent;

/**
 * Broadcasts events to WebSocket clients
 */
@ApplicationScoped
public class WebSocketEventBroadcaster {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketEventBroadcaster.class);

    @Inject
    WebSocketSessionManager sessionManager;

    @Inject
    WebSocketConnectionRegistry connectionRegistry;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Broadcast workflow execution event
     */
    public Uni<Void> broadcastWorkflowEvent(
            String tenantId,
            String runId,
            WorkflowEvent event) {

        LOG.debug("Broadcasting workflow event: {} for run {}",
                event.type(), runId);

        Set<String> subscribers = sessionManager.getSubscribers(
                "workflow", runId);

        if (subscribers.isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        String message = serializeEvent(event);

        return Uni.join().all(
                subscribers.stream()
                        .map(connectionId -> sendToConnection(connectionId, message))
                        .toList())
                .andFailFast()
                .replaceWithVoid();
    }

    /**
     * Broadcast agent event
     */
    public Uni<Void> broadcastAgentEvent(
            String tenantId,
            UUID agentId,
            AgentEvent event) {

        Set<String> subscribers = sessionManager.getSubscribers(
                "agent", agentId.toString());

        if (subscribers.isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        String message = serializeEvent(event);

        return Uni.join().all(
                subscribers.stream()
                        .map(connectionId -> sendToConnection(connectionId, message))
                        .toList())
                .andFailFast()
                .replaceWithVoid();
    }

    /**
     * Broadcast to all tenant connections
     */
    public Uni<Void> broadcastToTenant(String tenantId, Object event) {
        List<WebSocketSession> tenantSessions = sessionManager.getTenantSessions(tenantId);

        String message = serializeEvent(event);

        return Uni.join().all(
                tenantSessions.stream()
                        .map(session -> sendToConnection(session.connectionId(), message))
                        .toList())
                .andFailFast()
                .replaceWithVoid();
    }

    private Uni<Void> sendToConnection(String connectionId, String message) {
        WebSocketConnection connection = connectionRegistry.getConnection(connectionId);

        if (connection == null) {
            LOG.warn("Connection not found: {}", connectionId);
            return Uni.createFrom().voidItem();
        }

        return connection.sendText(message)
                .onFailure().invoke(error -> LOG.error("Failed to send to {}", connectionId, error));
    }

    private String serializeEvent(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            LOG.error("Failed to serialize event", e);
            return "{\"error\":\"Serialization failed\"}";
        }
    }
}
