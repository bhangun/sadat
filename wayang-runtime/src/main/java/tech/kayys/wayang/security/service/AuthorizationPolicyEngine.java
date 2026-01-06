package tech.kayys.wayang.security.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Authorization policy engine
 */
@ApplicationScoped
public class AuthorizationPolicyEngine {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationPolicyEngine.class);

    /**
     * Check if user can execute action on resource
     */
    public boolean authorize(
            AuthenticatedUser user,
            String resource,
            String action) {

        // Check role-based permissions
        String permission = resource + ":" + action;
        if (user.hasPermission(permission)) {
            return true;
        }

        // Check wildcard permissions
        String wildcardPermission = resource + ":*";
        if (user.hasPermission(wildcardPermission)) {
            return true;
        }

        // Check admin role
        if (user.hasRole("admin") || user.hasRole("tenant_admin")) {
            return true;
        }

        return false;
    }

    /**
     * Attribute-based access control
     */
    public boolean authorizeWithAttributes(
            AuthenticatedUser user,
            String resource,
            String action,
            Map<String, Object> resourceAttributes) {
        LOG.info("Authorizing user {} for resource {} with action {}", user.userId(), resource, action);
        // Basic RBAC check
        if (!authorize(user, resource, action)) {
            return false;
        }

        // ABAC checks

        // Department-based access
        if (resourceAttributes.containsKey("department")) {
            String resourceDept = (String) resourceAttributes.get("department");
            String userDept = (String) user.attributes().get("department");
            if (userDept != null && !userDept.equals(resourceDept)) {
                // User can only access resources in their department
                return user.hasRole("cross_department_access");
            }
        }

        // Sensitivity-based access
        if (resourceAttributes.containsKey("sensitivity")) {
            String sensitivity = (String) resourceAttributes.get("sensitivity");
            if ("high".equals(sensitivity)) {
                return user.hasPermission("access:high_sensitivity");
            }
        }

        return true;
    }
}
