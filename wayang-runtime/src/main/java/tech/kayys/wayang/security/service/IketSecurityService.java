package tech.kayys.wayang.security.service;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * ============================================================================
 * SILAT SECURITY AND AUTHORIZATION MODULE (IKET Trust Model)
 * ============================================================================
 * 
 * Security implementation focused on IKET Gateway integration:
 * - Direct JWT verification and claim extraction
 * - API Key management
 * - Role-Based Access Control (RBAC)
 * - Attribute-Based Access Control (ABAC)
 * - Multi-tenancy enforcement
 * - Audit logging
 */

// ==================== IKET INTEGRATION ====================

/**
 * IKET authentication and authorization service
 */
@ApplicationScoped
public class IketSecurityService {

    private static final Logger LOG = LoggerFactory.getLogger(IketSecurityService.class);

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    JsonWebToken jwt;

    @Inject
    TenantResolver tenantResolver;

    /**
     * Get current authenticated user
     */
    public AuthenticatedUser getCurrentUser() {
        if (securityIdentity.isAnonymous()) {
            throw new SecurityException("User not authenticated");
        }

        String userId = securityIdentity.getPrincipal().getName();
        Set<String> roles = securityIdentity.getRoles();

        // Handle mTLS identity
        String authType = securityIdentity.getAttribute("auth_type");
        if ("mtls".equals(authType)) {
            String tenantId = securityIdentity.getAttribute("tenant_id");
            if (tenantId == null) {
                tenantId = "default"; // Fallback for mTLS if tenant not provided in header
            }
            String clientCn = securityIdentity.getAttribute("client_cn");

            return new AuthenticatedUser(
                    userId,
                    clientCn != null ? clientCn : userId,
                    null,
                    tenantId,
                    roles,
                    Set.of(), // Permissions would need to be handled separately for mTLS
                    Map.of("auth_type", "mtls"));
        }

        // Extract tenant from JWT claims
        String tenantId = jwt.<String>claim("tenant_id")
                .orElseThrow(() -> new SecurityException("Tenant ID not found in token"));

        // Extract additional claims
        String email = jwt.<String>claim("email").orElse(null);
        String name = jwt.<String>claim("name").orElse(userId);
        LOG.debug("User: {}", userId);
        return new AuthenticatedUser(
                userId,
                name,
                email,
                tenantId,
                roles,
                extractPermissions(),
                extractAttributes());
    }

    /**
     * Extract permissions from JWT
     */
    private Set<String> extractPermissions() {
        Set<String> permissions = new HashSet<>();

        // Extract from resource_access claim
        Map<String, Object> resourceAccess = jwt.<Map<String, Object>>claim("resource_access").orElse(null);
        if (resourceAccess != null) {
            Object silatResource = resourceAccess.get("silat");
            if (silatResource instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> silatMap = (Map<String, Object>) silatResource;
                Object rolesObj = silatMap.get("roles");
                if (rolesObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> roles = (List<String>) rolesObj;
                    permissions.addAll(roles);
                }
            }
        }

        // Extract from custom permissions claim
        List<String> customPerms = jwt.<List<String>>claim("permissions").orElse(null);
        if (customPerms != null) {
            permissions.addAll(customPerms);
        }

        return permissions;
    }

    /**
     * Extract custom attributes
     */
    private Map<String, Object> extractAttributes() {
        Map<String, Object> attributes = new HashMap<>();

        // Organization
        jwt.<String>claim("organization_id").ifPresent(
                org -> attributes.put("organization_id", org));

        // Department
        jwt.<String>claim("department").ifPresent(
                dept -> attributes.put("department", dept));

        // Custom attributes
        jwt.<Map<String, Object>>claim("custom_attributes").ifPresent(
                attrs -> attributes.putAll(attrs));

        return attributes;
    }

    /**
     * Validate user has required role
     */
    public boolean hasRole(String role) {
        return securityIdentity.hasRole(role);
    }

    /**
     * Validate user has required permission
     */
    public boolean hasPermission(String permission) {
        return extractPermissions().contains(permission);
    }

    /**
     * Validate user can access tenant resources
     */
    public boolean canAccessTenant(String tenantId) {
        AuthenticatedUser user = getCurrentUser();
        return user.tenantId().equals(tenantId);
    }
}
