package tech.kayys.wayang.saml.dto;

import java.util.Map;

import tech.kayys.wayang.billing.model.SamlProvider;

public record SamlConfigurationRequest(
    SamlProvider provider,
    String entityId,
    String ssoUrl,
    String sloUrl,
    String x509Certificate,
    boolean signRequests,
    boolean encryptAssertions,
    Map<String, String> attributeMapping,
    boolean jitProvisioning
) {}
