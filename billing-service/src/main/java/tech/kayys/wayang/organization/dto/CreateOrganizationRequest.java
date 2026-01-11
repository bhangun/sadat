package tech.kayys.wayang.organization.dto;

import java.util.Map;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import tech.kayys.wayang.billing.domain.Address;
import tech.kayys.wayang.organization.model.OrganizationType;

public record CreateOrganizationRequest(
    @NotNull String name,
    @NotNull String slug,
    @Email String billingEmail,
    Address billingAddress,
    OrganizationType orgType,
    String taxId,
    String ktp,
    Map<String, Object> metadata
) {}
