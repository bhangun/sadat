package tech.kayys.wayang.agent.service;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.dto.AgentPrincipal;

/**
 * Thread-safe security context
 */
@ApplicationScoped
public class SecurityContext {

    private final ThreadLocal<AgentPrincipal> principalHolder = new ThreadLocal<>();

    public void setPrincipal(AgentPrincipal principal) {
        principalHolder.set(principal);
    }

    public AgentPrincipal getPrincipal() {
        return principalHolder.get();
    }

    public void clear() {
        principalHolder.remove();
    }

    public String getCurrentTenantId() {
        AgentPrincipal principal = getPrincipal();
        return principal != null ? principal.tenantId() : null;
    }

    public String getCurrentUserId() {
        AgentPrincipal principal = getPrincipal();
        return principal != null ? principal.userId() : null;
    }

    public boolean hasPermission(String permission) {
        AgentPrincipal principal = getPrincipal();
        return principal != null && principal.permissions().contains(permission);
    }

    public boolean hasRole(String role) {
        AgentPrincipal principal = getPrincipal();
        return principal != null && principal.roles().contains(role);
    }
}
