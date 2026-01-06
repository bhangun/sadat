package tech.kayys.wayang.security.service;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Multi-tenant context resolver
 */
@ApplicationScoped
public class TenantResolver {

    private static final Logger LOG = LoggerFactory.getLogger(TenantResolver.class);

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    JsonWebToken jwt;

    /**
     * Resolve tenant ID from current context
     */
    public String resolveTenantId() {
        LOG.info("Resolving tenant ID");
        if (!securityIdentity.isAnonymous()) {
            // From JWT token
            return jwt.<String>claim("tenant_id")
                    .orElseThrow(() -> new SecurityException("Tenant ID not found"));
        }

        // From API key (set by filter)
        String tenantId = io.vertx.core.Vertx.currentContext()
                .getLocal("tenant_id");
        if (tenantId != null) {
            return tenantId;
        }

        throw new SecurityException("Unable to resolve tenant ID");
    }
}
