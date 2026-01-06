package tech.kayys.wayang.security.service;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * Authenticated user information
 */
public record AuthenticatedUser(
        String userId,
        String name,
        String email,
        String tenantId,
        Set<String> roles,
        Set<String> permissions,
        Map<String, Object> attributes) {
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public boolean hasAnyRole(String... roles) {
        return Arrays.stream(roles).anyMatch(this.roles::contains);
    }

    public boolean hasAllRoles(String... roles) {
        return Arrays.stream(roles).allMatch(this.roles::contains);
    }
}
