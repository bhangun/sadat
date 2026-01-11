package tech.kayys.silat.executor.camel.modern;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket real-time integration service
 */
@ApplicationScoped
public class WebSocketIntegrationService {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketIntegrationService.class);

    private final Map<String, WebSocketClient> activeConnections = new ConcurrentHashMap<>();

    /**
     * Connect to WebSocket endpoint and stream messages
     */
    public Multi<WebSocketMessage> connect(
            String websocketUrl,
            Map<String, String> headers,
            String tenantId) {

        return Multi.createFrom().emitter(emitter -> {
            try {
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();

                // Configure WebSocket client
                ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
                    .configurator(new ClientEndpointConfig.Configurator() {
                        @Override
                        public void beforeRequest(Map<String, List<String>> headersMap) {
                            headers.forEach((key, value) ->
                                headersMap.put(key, List.of(value))
                            );
                        }
                    })
                    .build();

                Session session = container.connectToServer(
                    new WebSocketMessageEndpoint(emitter, tenantId),
                    config,
                    URI.create(websocketUrl)
                );

                String connectionId = UUID.randomUUID().toString();
                activeConnections.put(connectionId,
                    new WebSocketClient(session, emitter, tenantId)
                );

                emitter.onTermination(() -> {
                    try {
                        session.close();
                        activeConnections.remove(connectionId);
                    } catch (Exception e) {
                        LOG.error("Error closing WebSocket", e);
                    }
                });

            } catch (Exception e) {
                LOG.error("WebSocket connection failed", e);
                emitter.fail(e);
            }
        });
    }

    /**
     * Send message to WebSocket
     */
    public Uni<Void> send(String connectionId, String message) {
        WebSocketClient client = activeConnections.get(connectionId);

        if (client != null) {
            return Uni.createFrom().completionStage(() ->
                CompletableFuture.runAsync(() -> {
                    try {
                        client.session().getBasicRemote().sendText(message);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to send message", e);
                    }
                })
            );
        }

        return Uni.createFrom().failure(
            new IllegalStateException("Connection not found: " + connectionId)
        );
    }

    /**
     * Close WebSocket connection
     */
    public Uni<Void> close(String connectionId) {
        WebSocketClient client = activeConnections.remove(connectionId);

        if (client != null) {
            return Uni.createFrom().completionStage(() ->
                CompletableFuture.runAsync(() -> {
                    try {
                        client.session().close();
                        client.emitter().complete();
                    } catch (Exception e) {
                        LOG.error("Error closing connection", e);
                    }
                })
            );
        }

        return Uni.createFrom().voidItem();
    }
}