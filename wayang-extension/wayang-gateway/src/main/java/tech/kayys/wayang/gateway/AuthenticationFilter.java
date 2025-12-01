package tech.kayys.wayang.gateway.filter;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.jwt.auth.principal.JWTParser;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.*;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

@Provider
@PreMatching
@Priority(1000)
public class AuthenticationFilter implements ContainerRequestFilter {
    
    private static final Logger LOG = Logger.getLogger(AuthenticationFilter.class);
    
    @Inject
    JWTParser jwtParser;
    
    @Inject
    SecurityIdentity identity;
    
    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        
        // Skip auth for health checks
        if (path.startsWith("/q/health")) {
            return;
        }
        
        String authHeader = requestContext.getHeaderString("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Missing or invalid Authorization header")
                    .build()
            );
            return;
        }
        
        String token = authHeader.substring(7);
        
        try {
            JsonWebToken jwt = jwtParser.parse(token);
            
            // Inject tenant ID into headers for downstream services
            String tenantId = jwt.getClaim("tenant_id");
            requestContext.getHeaders().putSingle("X-Tenant-ID", tenantId);
            
            // Inject user ID
            requestContext.getHeaders().putSingle("X-User-ID", jwt.getSubject());
            
            LOG.debugf("Authenticated request for tenant: %s, user: %s", 
                       tenantId, jwt.getSubject());
            
        } catch (Exception e) {
            LOG.error("JWT validation failed", e);
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid token")
                    .build()
            );
        }
    }
}