package tech.kayys.wayang.security.service;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.security.MtlsSecurityConfig;

import java.util.HashSet;
import java.util.Set;

/**
 * Augments SecurityIdentity with mTLS information if enabled and present
 */
@ApplicationScoped
public class MtlsIdentityAugmentor implements SecurityIdentityAugmentor {

    private static final Logger LOG = LoggerFactory.getLogger(MtlsIdentityAugmentor.class);

    @Inject
    MtlsSecurityConfig config;

    @Inject
    Instance<io.vertx.ext.web.RoutingContext> routingContextInstance;

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        if (!config.enabled() || !routingContextInstance.isResolvable()) {
            return Uni.createFrom().item(identity);
        }

        io.vertx.ext.web.RoutingContext routingContext = routingContextInstance.get();

        // Try to extract identity from headers (sent by API Gateway)
        String clientCn = routingContext.request().getHeader(config.headerName());
        String tenantId = routingContext.request().getHeader(config.tenantIdHeader());

        if (clientCn != null && !clientCn.isBlank()) {
            LOG.debug("Augmenting identity with mTLS CN: {} and Tenant: {}", clientCn, tenantId);

            return Uni.createFrom().item(() -> {
                QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);

                // Set principal if anonymous
                if (identity.isAnonymous()) {
                    builder.setPrincipal(new QuarkusPrincipal(clientCn));
                }

                // Add mTLS specific attributes
                builder.addAttribute("auth_type", "mtls");
                builder.addAttribute("client_cn", clientCn);
                if (tenantId != null) {
                    builder.addAttribute("tenant_id", tenantId);
                }

                // Add default roles for mTLS
                Set<String> roles = new HashSet<>(identity.getRoles());
                roles.addAll(config.defaultRoles());
                builder.addRoles(roles);

                return builder.build();
            });
        }

        return Uni.createFrom().item(identity);
    }
}
