package tech.kayys.wayang.websocket.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Registry of active WebSocket connections
 */
@ApplicationScoped
public class WebSocketConnectionRegistry {

    private final Map<String, WebSocketConnection> connections = new ConcurrentHashMap<>();

    public void register(String connectionId, WebSocketConnection connection) {
        connections.put(connectionId, connection);
    }

    public void unregister(String connectionId) {
        connections.remove(connectionId);
    }

    public WebSocketConnection getConnection(String connectionId) {
        return connections.get(connectionId);
    }
}