package tech.kayys.wayang.saml.dto;

import java.util.List;

public record SamlAssertion(
    String tenantId,
    String email,
    String firstName,
    String lastName,
    List<String> groups
) {}
