package tech.kayys.wayang.saml.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import tech.kayys.wayang.organization.domain.Organization;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * SAML configuration entity
 */
@Entity
@Table(name = "sso_saml_configurations")
public class SamlConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID configId;

    @OneToOne
    @JoinColumn(name = "organization_id")
    public Organization organization;

    @Column(name = "enabled")
    public boolean enabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider")
    public SamlProvider provider;

    @Column(name = "entity_id")
    public String entityId;

    @Column(name = "sso_url")
    public String ssoUrl;

    @Column(name = "slo_url")
    public String sloUrl;

    @Column(name = "x509_certificate", columnDefinition = "text")
    public String x509Certificate;

    @Column(name = "sign_requests")
    public boolean signRequests = true;

    @Column(name = "encrypt_assertions")
    public boolean encryptAssertions = true;

    @Column(name = "attribute_mapping", columnDefinition = "jsonb")
    public Map<String, String> attributeMapping;

    @Column(name = "just_in_time_provisioning")
    public boolean jitProvisioning = true;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;
}