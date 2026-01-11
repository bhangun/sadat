package tech.kayys.wayang.saml.service;

import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.billing.dto.SsoAuthenticationResult;
import tech.kayys.wayang.organization.domain.Organization;
import tech.kayys.wayang.organization.domain.OrganizationUser;
import tech.kayys.wayang.saml.domain.SamlConfiguration;
import tech.kayys.wayang.saml.dto.SamlAssertion;
import tech.kayys.wayang.saml.dto.SamlConfigurationRequest;
import tech.kayys.wayang.billing.model.SamlProvider;
import tech.kayys.wayang.saml.service.SamlAuthenticator;
import tech.kayys.wayang.saml.service.UserProvisioningService;
import tech.kayys.wayang.billing.dto.SsoAuthenticationResult;

/**
 * SSO service
 */
@ApplicationScoped
public class SsoService {

    private static final Logger LOG = LoggerFactory.getLogger(SsoService.class);

    @Inject
    SamlAuthenticator samlAuthenticator;

    @Inject
    UserProvisioningService provisioningService;

    /**
     * Configure SAML for organization
     */
    public Uni<SamlConfiguration> configureSaml(
            UUID organizationId,
            SamlConfigurationRequest request) {

        LOG.info("Configuring SAML for organization: {}", organizationId);

        return Organization.<Organization>findById(organizationId)
                .flatMap(org -> {
                    if (org == null) {
                        return Uni.createFrom().failure(
                                new NoSuchElementException("Organization not found"));
                    }

                    // Validate certificate
                    if (!validateCertificate(request.x509Certificate())) {
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("Invalid X.509 certificate"));
                    }

                    return io.quarkus.hibernate.reactive.panache.Panache.withTransaction(() -> {
                        SamlConfiguration config = new SamlConfiguration();
                        config.organization = org;
                        config.enabled = false; // Enable after testing
                        config.provider = request.provider();
                        config.entityId = request.entityId();
                        config.ssoUrl = request.ssoUrl();
                        config.sloUrl = request.sloUrl();
                        config.x509Certificate = request.x509Certificate();
                        config.signRequests = request.signRequests();
                        config.encryptAssertions = request.encryptAssertions();
                        config.attributeMapping = request.attributeMapping() != null ? request.attributeMapping()
                                : getDefaultAttributeMapping();
                        config.jitProvisioning = request.jitProvisioning();
                        config.createdAt = Instant.now();
                        config.updatedAt = Instant.now();

                        return config.persist()
                                .map(v -> config);
                    });
                });
    }

    /**
     * Authenticate user via SAML
     */
    public Uni<SsoAuthenticationResult> authenticateSaml(
            String samlResponse,
            String relayState) {

        LOG.debug("Processing SAML authentication");

        return samlAuthenticator.validateResponse(samlResponse)
                .flatMap(assertion -> {
                    String tenantId = assertion.tenantId();
                    String email = assertion.email();

                    return SamlConfiguration.<SamlConfiguration>find(
                            "organization.tenantId = ?1 and enabled = true",
                            tenantId).firstResult()
                            .flatMap(config -> {
                                if (config == null) {
                                    return Uni.createFrom().failure(
                                            new IllegalStateException("SAML not configured"));
                                }

                                // Check if user exists
                                return findOrCreateUser(config, assertion);
                            })
                            .map(user -> new SsoAuthenticationResult(
                                    true,
                                    user.userId.toString(),
                                    user.email,
                                    generateSessionToken(user)));
                })
                .onFailure().recoverWithItem(error -> {
                    LOG.error("SAML authentication failed", error);
                    return new SsoAuthenticationResult(
                            false,
                            null,
                            null,
                            null);
                });
    }

    /**
     * Find or create user (JIT provisioning)
     */
    private Uni<OrganizationUser> findOrCreateUser(
            SamlConfiguration config,
            SamlAssertion assertion) {

        return OrganizationUser.<OrganizationUser>find(
                "organization = ?1 and email = ?2",
                config.organization,
                assertion.email()).firstResult()
                .flatMap(existingUser -> {
                    if (existingUser != null) {
                        return Uni.createFrom().item(existingUser);
                    }

                    // JIT provisioning
                    if (!config.jitProvisioning) {
                        return Uni.createFrom().failure(
                                new IllegalStateException("User not found and JIT provisioning disabled"));
                    }

                    return provisioningService.provisionUserFromSaml(
                            config.organization,
                            assertion);
                });
    }

    private boolean validateCertificate(String certificate) {
        // TODO: Validate X.509 certificate
        return certificate != null && certificate.contains("BEGIN CERTIFICATE");
    }

    private Map<String, String> getDefaultAttributeMapping() {
        return Map.of(
                "email", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress",
                "firstName", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname",
                "lastName", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname",
                "groups", "http://schemas.xmlsoap.org/claims/Group");
    }

    private String generateSessionToken(OrganizationUser user) {
        // Generate JWT token
        return "jwt_token_here";
    }
}
