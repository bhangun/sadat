package tech.kayys.wayang.tenant;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import org.jboss.logging.Logger;

import java.util.Optional;

/**
 * TenantContext - Request-scoped tenant information
 */
@RequestScoped
public class TenantContext {

    private static final Logger LOG = Logger.getLogger(TenantContext.class);

    private String tenantId;
    private String userId;
    private SecurityIdentity identity;

    public String getTenantId() {
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID not set in context");
        }
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUserId() {
        if (userId == null) {
            throw new IllegalStateException("User ID not set in context");
        }
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public SecurityIdentity getIdentity() {
        return identity;
    }

    public void setIdentity(SecurityIdentity identity) {
        this.identity = identity;
    }

    public boolean hasRole(String role) {
        return identity != null && identity.hasRole(role);
    }

    public Optional<String> getClaim(String claimName) {
        if (identity == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(identity.getAttribute(claimName));
    }
}
