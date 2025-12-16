package tech.kayys.wayang.config;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * SecurityConfig - Security configuration
 */

/**
 * RoleMapper - Map JWT roles to application roles
 */
@ApplicationScoped
public class RoleMapper {

    /**
     * Map external roles to internal roles
     */
    public boolean hasAccess(SecurityIdentity identity, String requiredRole) {
        if (identity == null || identity.isAnonymous()) {
            return false;
        }

        // Direct role match
        if (identity.hasRole(requiredRole)) {
            return true;
        }

        // Map roles
        return switch (requiredRole) {
            case "designer" -> identity.hasRole("workflow-designer") ||
                    identity.hasRole("admin");
            case "viewer" -> identity.hasRole("workflow-viewer") ||
                    identity.hasRole("designer") ||
                    identity.hasRole("admin");
            default -> false;
        };
    }
}
