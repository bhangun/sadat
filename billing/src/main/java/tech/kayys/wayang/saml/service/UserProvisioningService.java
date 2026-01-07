package tech.kayys.wayang.saml.service;

import java.time.Instant;
import java.util.UUID;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.organization.domain.Organization;
import tech.kayys.wayang.organization.domain.OrganizationUser;
import tech.kayys.wayang.saml.dto.SamlAssertion;

/**
 * User provisioning service
 */
@ApplicationScoped
public class UserProvisioningService {
    
    public Uni<OrganizationUser> provisionUserFromSaml(
            Organization organization,
            SamlAssertion assertion) {
        
        return io.quarkus.hibernate.reactive.panache.Panache.withTransaction(() -> {
            OrganizationUser user = new OrganizationUser();
            user.organization = organization;
            user.userId = UUID.randomUUID().toString();
            user.email = assertion.email();
            user.firstName = assertion.firstName();
            user.lastName = assertion.lastName();
            user.createdAt = Instant.now();
            user.active = true;
            user.ssoProvisioned = true;
            
            return user.persist()
                .map(v -> user);
        });
    }
}
