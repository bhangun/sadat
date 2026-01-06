package tech.kayys.wayang.websocket.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.security.service.ApiKeyService;
import tech.kayys.wayang.security.service.AuthenticatedUser;
import tech.kayys.wayang.security.service.KeycloakSecurityService;
import tech.kayys.wayang.websocket.dto.WebSocketSession;

/**
 * WebSocket authenticator
 */
@ApplicationScoped
public class WebSocketAuthenticator {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketAuthenticator.class);

    @Inject
    ApiKeyService apiKeyService;

    @Inject
    KeycloakSecurityService keycloakService;

    public Uni<WebSocketSession> authenticate(String token) {
        LOG.debug("Authenticating WebSocket connection with token: {}", token);
        if (token == null) {
            return Uni.createFrom().failure(
                    new SecurityException("Token required"));
        }

        // Try API key first
        if (token.startsWith("sk_")) {
            return authenticateApiKey(token);
        }

        // Try JWT token
        return authenticateJwt(token);
    }

    private Uni<WebSocketSession> authenticateApiKey(String apiKey) {
        return apiKeyService.validateApiKey(apiKey)
                .map(validation -> {
                    if (!validation.valid()) {
                        throw new SecurityException(validation.error());
                    }

                    return new WebSocketSession(
                            UUID.randomUUID().toString(),
                            validation.tenantId(),
                            "api_key",
                            null,
                            new HashSet<>(validation.scopes()),
                            Instant.now(),
                            Map.of("auth_type", "api_key"));
                });
    }

    private Uni<WebSocketSession> authenticateJwt(String jwt) {
        // In production, validate JWT properly
        // For now, create session from current security context
        return Uni.createFrom().item(() -> {
            try {
                AuthenticatedUser user = keycloakService.getCurrentUser();
                return new WebSocketSession(
                        UUID.randomUUID().toString(),
                        user.tenantId(),
                        user.userId(),
                        user.email(),
                        user.permissions(),
                        Instant.now(),
                        Map.of("auth_type", "jwt"));
            } catch (Exception e) {
                throw new SecurityException("Invalid JWT token");
            }
        });
    }
}
