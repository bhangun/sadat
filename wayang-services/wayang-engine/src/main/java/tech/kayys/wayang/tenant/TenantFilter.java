package tech.kayys.wayang.tenant;

import org.jboss.logging.Logger;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.ws.rs.Priorities;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * TenantFilter - Extract tenant information from request
 */
@Provider
@Priority(Priorities.AUTHENTICATION + 1)
public class TenantFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(TenantFilter.class);
    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String USER_ID_CLAIM = "sub";

    @Inject
    TenantContext tenantContext;

    @Inject
    SecurityIdentity securityIdentity;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Skip for health/metrics endpoints
        String path = requestContext.getUriInfo().getPath();
        if (path.startsWith("health") || path.startsWith("metrics") || path.startsWith("q/")) {
            return;
        }

        // Extract tenant ID from header
        String tenantId = requestContext.getHeaderString(TENANT_HEADER);

        if (tenantId == null || tenantId.isBlank()) {
            LOG.error("Missing tenant ID in request header");
            requestContext.abortWith(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity(new ErrorResponse(
                                    "MISSING_TENANT_ID",
                                    "X-Tenant-Id header is required"))
                            .build());
            return;
        }

        // Extract user ID from security identity
        String userId = securityIdentity.getPrincipal() != null ? securityIdentity.getPrincipal().getName()
                : "anonymous";

        // Set context
        tenantContext.setTenantId(tenantId);
        tenantContext.setUserId(userId);
        tenantContext.setIdentity(securityIdentity);

        LOG.debugf("Tenant context set: tenantId=%s, userId=%s", tenantId, userId);
    }

    record ErrorResponse(String code, String message) {
    }
}
