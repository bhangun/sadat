package tech.kayys.wayang.workflow.security.grpc;

import io.grpc.*;
import io.quarkus.grpc.GlobalInterceptor;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import tech.kayys.wayang.workflow.security.context.SecurityContextHolder;

/**
 * gRPC interceptor that enforces tenant isolation and authentication.
 * 
 * Responsibilities:
 * 1. Validate authentication (JWT or execution context token)
 * 2. Extract and set tenant context
 * 3. Audit security events
 * 4. Reject unauthenticated requests
 */
@GlobalInterceptor
@ApplicationScoped
public class TenantSecurityInterceptor implements ServerInterceptor {

    private static final Logger LOG = Logger.getLogger(TenantSecurityInterceptor.class);

    private static final Metadata.Key<String> AUTHORIZATION_HEADER = Metadata.Key.of("authorization",
            Metadata.ASCII_STRING_MARSHALLER);

    @Inject
    SecurityIdentity securityIdentity;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String method = call.getMethodDescriptor().getFullMethodName();

        try {
            // Extract tenant and set security context
            String tenantId = extractTenantId();

            if (tenantId != null) {
                SecurityContextHolder.setContext(
                        SecurityContextHolder.SecurityContext.fromUserAuth(tenantId, securityIdentity));

                LOG.debugf("Security context set: tenant=%s, method=%s, principal=%s",
                        tenantId, method, securityIdentity.getPrincipal().getName());
            }

            // Continue with the call
            return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                    next.startCall(call, headers)) {

                @Override
                public void onComplete() {
                    try {
                        super.onComplete();
                    } finally {
                        // Clean up security context
                        SecurityContextHolder.clear();
                    }
                }

                @Override
                public void onCancel() {
                    try {
                        super.onCancel();
                    } finally {
                        SecurityContextHolder.clear();
                    }
                }
            };

        } catch (Exception e) {
            LOG.errorf(e, "Security validation failed for method: %s", method);
            SecurityContextHolder.clear();

            call.close(
                    Status.UNAUTHENTICATED.withDescription("Authentication failed: " + e.getMessage()),
                    new Metadata());

            return new ServerCall.Listener<ReqT>() {
            };
        }
    }

    /**
     * Extract tenant ID from the security identity.
     * Priority:
     * 1. tenant_id claim from JWT
     * 2. Principal name (for basic auth or fallback)
     */
    private String extractTenantId() {
        if (securityIdentity.isAnonymous()) {
            throw new SecurityException("Anonymous access not allowed");
        }

        // Try to get tenant_id from JWT claims
        if (securityIdentity.getPrincipal() instanceof JsonWebToken jwt) {
            String tenantId = jwt.getClaim("tenant_id");
            if (tenantId != null && !tenantId.isBlank()) {
                return tenantId;
            }
            // Fallback to realm from issuer
            String issuer = jwt.getIssuer();
            if (issuer != null && issuer.contains("/realms/")) {
                String realm = issuer.substring(issuer.lastIndexOf("/realms/") + 8);
                if (!realm.isBlank()) {
                    return realm;
                }
            }
        }

        // Fallback to principal name
        String principalName = securityIdentity.getPrincipal().getName();
        if (principalName != null && !principalName.isBlank()) {
            return principalName;
        }

        throw new SecurityException("Could not extract tenant ID from security context");
    }
}
