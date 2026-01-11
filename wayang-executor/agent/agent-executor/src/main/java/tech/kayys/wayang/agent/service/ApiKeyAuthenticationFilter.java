package tech.kayys.wayang.agent.service;

import java.io.IOException;
import io.quarkus.security.UnauthorizedException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import tech.kayys.wayang.agent.dto.AgentPrincipal;
import tech.kayys.wayang.agent.dto.ApiKeyValidationResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ============================================================================
 * SECURITY AND AUTHENTICATION LAYER
 * ============================================================================
 * 
 * Production-ready security features:
 * - API key authentication
 * - Rate limiting per tenant/user
 * - Request validation and sanitization
 * - Tool execution sandboxing
 * - Audit logging
 * - Secret management
 * - Token-based authentication (JWT)
 * 
 * Architecture:
 * ┌────────────────────────────────────────────────────────┐
 * │              Security Filter Chain                      │
 * ├────────────────────────────────────────────────────────┤
 * │  Authentication → Authorization → Rate Limiting         │
 * │       ↓                ↓               ↓               │
 * │  API Key Check → Tenant Check → Quota Check            │
 * └────────────────────────────────────────────────────────┘
 */
/**
 * API Key Authentication Filter
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyAuthenticationFilter implements ContainerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String TENANT_ID_HEADER = "X-Tenant-ID";

    @Inject
    ApiKeyValidator apiKeyValidator;

    @Inject
    SecurityContext securityContext;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();

        // Skip authentication for health checks and public endpoints
        if (isPublicEndpoint(path)) {
            return;
        }

        String apiKey = requestContext.getHeaderString(API_KEY_HEADER);
        String tenantId = requestContext.getHeaderString(TENANT_ID_HEADER);

        if (apiKey == null || apiKey.isEmpty()) {
            LOG.warn("Missing API key for request: {}", path);
            throw new UnauthorizedException("API key required");
        }

        if (tenantId == null || tenantId.isEmpty()) {
            LOG.warn("Missing tenant ID for request: {}", path);
            throw new UnauthorizedException("Tenant ID required");
        }

        // Validate API key
        ApiKeyValidationResult validation = apiKeyValidator
                .validate(apiKey, tenantId)
                .await().indefinitely();

        if (!validation.isValid()) {
            LOG.warn("Invalid API key for tenant: {}", tenantId);
            throw new UnauthorizedException("Invalid API key");
        }

        // Check if key is active
        if (!validation.isActive()) {
            LOG.warn("Inactive API key used for tenant: {}", tenantId);
            throw new UnauthorizedException("API key is inactive");
        }

        // Set security context
        securityContext.setPrincipal(new AgentPrincipal(
                validation.userId(),
                tenantId,
                validation.roles(),
                validation.permissions()));

        LOG.trace("Authenticated request for tenant: {} user: {}",
                tenantId, validation.userId());
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/health") ||
                path.startsWith("/metrics") ||
                path.startsWith("/q/");
    }
}
