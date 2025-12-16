package tech.kayys.wayang.websocket;

import java.util.List;

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * WebSocketConfigurator - Configure WebSocket with security
 */
public class WebSocketConfigurator extends ServerEndpointConfig.Configurator {

    @Override
    public void modifyHandshake(ServerEndpointConfig config,
            HandshakeRequest request, HandshakeResponse response) {

        // Extract headers
        String tenantId = getHeader(request, "X-Tenant-Id");
        String userId = getHeader(request, "X-User-Id");
        String userName = getHeader(request, "X-User-Name");

        // Store in user properties
        config.getUserProperties().put("tenantId", tenantId);
        config.getUserProperties().put("userId", userId);
        config.getUserProperties().put("userName", userName);
    }

    private String getHeader(HandshakeRequest request, String headerName) {
        List<String> values = request.getHeaders().get(headerName);
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }
}
