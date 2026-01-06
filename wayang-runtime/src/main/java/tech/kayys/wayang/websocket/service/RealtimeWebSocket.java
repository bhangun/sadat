package tech.kayys.wayang.websocket.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.websocket.dto.CommandResponse;
import jakarta.inject.Inject;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import tech.kayys.wayang.websocket.dto.ErrorResponse;
import tech.kayys.wayang.websocket.dto.PongResponse;
import tech.kayys.wayang.websocket.dto.SubscribeResponse;
import tech.kayys.wayang.websocket.dto.UnsubscribeResponse;
import tech.kayys.wayang.websocket.dto.WebSocketMessage;
import tech.kayys.wayang.websocket.dto.WebSocketSession;
import tech.kayys.wayang.websocket.dto.WelcomeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * ============================================================================
 * SILAT WEBSOCKET API
 * ============================================================================
 * 
 * Real-time bidirectional communication for:
 * - Workflow execution updates
 * - Agent activity streaming
 * - Control plane notifications
 * - Live monitoring and debugging
 */

// ==================== WEBSOCKET ENDPOINTS ====================

/**
 * Main WebSocket endpoint for real-time updates
 */
@WebSocket(path = "/ws/v1/realtime")
public class RealtimeWebSocket {

    private static final Logger LOG = LoggerFactory.getLogger(RealtimeWebSocket.class);

    @Inject
    WebSocketSessionManager sessionManager;

    @Inject
    WebSocketAuthenticator authenticator;

    @Inject
    ObjectMapper objectMapper;

    @OnOpen
    public Uni<Void> onOpen(WebSocketConnection connection) {
        LOG.info("WebSocket connection attempt from: {}",
                connection.handshakeRequest().path());

        // Authenticate via query param or header
        String token = extractToken(connection);

        return authenticator.authenticate(token)
                .flatMap(session -> {
                    sessionManager.registerSession(connection.id(), session);

                    // Send welcome message
                    WelcomeMessage welcome = new WelcomeMessage(
                            "connected",
                            session.tenantId(),
                            session.userId(),
                            Instant.now());

                    return connection.sendText(toJson(welcome));
                })
                .onFailure().invoke(error -> {
                    LOG.error("Authentication failed", error);
                    connection.close();
                });
    }

    @OnClose
    public void onClose(WebSocketConnection connection) {
        LOG.info("WebSocket closed: {}", connection.id());
        sessionManager.unregisterSession(connection.id());
    }

    @OnTextMessage
    public Uni<Void> onMessage(
            String message,
            WebSocketConnection connection) {

        LOG.debug("Received message: {}", message);

        return Uni.createFrom().item(() -> {
            try {
                return objectMapper.readValue(message, WebSocketMessage.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse message", e);
            }
        })
                .flatMap(msg -> handleMessage(msg, connection))
                .onFailure().invoke(error -> LOG.error("Failed to process message", error));
    }

    @OnError
    public void onError(WebSocketConnection connection, Throwable error) {
        LOG.error("WebSocket error on connection: {}",
                connection.id(), error);
    }

    // ==================== MESSAGE HANDLING ====================

    private Uni<Void> handleMessage(
            WebSocketMessage message,
            WebSocketConnection connection) {

        WebSocketSession session = sessionManager.getSession(connection.id());
        if (session == null) {
            return connection.sendText(
                    toJson(new ErrorResponse("Session not found")));
        }

        return switch (message.type()) {
            case "subscribe" -> handleSubscribe(message, session, connection);
            case "unsubscribe" -> handleUnsubscribe(message, session, connection);
            case "ping" -> handlePing(connection);
            case "command" -> handleCommand(message, session, connection);
            default -> connection.sendText(
                    toJson(new ErrorResponse("Unknown message type: " + message.type())));
        };
    }

    private Uni<Void> handleSubscribe(
            WebSocketMessage message,
            WebSocketSession session,
            WebSocketConnection connection) {

        String channel = (String) message.payload().get("channel");
        String resourceId = (String) message.payload().get("resourceId");

        LOG.info("Subscribe to {} for resource {}", channel, resourceId);

        sessionManager.subscribe(
                connection.id(),
                channel,
                resourceId);

        SubscribeResponse response = new SubscribeResponse(
                "subscribed",
                channel,
                resourceId);

        return connection.sendText(toJson(response));
    }

    private Uni<Void> handleUnsubscribe(
            WebSocketMessage message,
            WebSocketSession session,
            WebSocketConnection connection) {

        String channel = (String) message.payload().get("channel");
        String resourceId = (String) message.payload().get("resourceId");

        sessionManager.unsubscribe(connection.id(), channel, resourceId);

        return connection.sendText(
                toJson(new UnsubscribeResponse("unsubscribed", channel, resourceId)));
    }

    private Uni<Void> handlePing(WebSocketConnection connection) {
        return connection.sendText(
                toJson(new PongResponse("pong", Instant.now())));
    }

    private Uni<Void> handleCommand(
            WebSocketMessage message,
            WebSocketSession session,
            WebSocketConnection connection) {

        // Handle commands like pause, resume, cancel
        String command = (String) message.payload().get("command");
        String targetId = (String) message.payload().get("targetId");

        LOG.info("Command {} for target {}", command, targetId);

        // Execute command and send response
        return connection.sendText(
                toJson(new CommandResponse("executed", command, targetId)));
    }

    // ==================== HELPERS ====================

    private String extractToken(WebSocketConnection connection) {
        // Try query parameter
        String token = connection.handshakeRequest().query() != null
                && connection.handshakeRequest().query().contains("token=")
                        ? connection.handshakeRequest().query().split("token=")[1].split("&")[0]
                        : null;

        if (token == null) {
            // Try header
            token = connection.handshakeRequest().header("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
        }

        return token;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            LOG.error("Failed to serialize object", e);
            return "{\"error\":\"Serialization failed\"}";
        }
    }
}