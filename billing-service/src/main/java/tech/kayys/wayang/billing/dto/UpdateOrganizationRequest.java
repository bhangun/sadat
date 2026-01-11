package tech.kayys.wayang.billing.dto;

import tech.kayys.wayang.billing.domain.Address;
import tech.kayys.wayang.organization.domain.OrganizationSettings;

public record UpdateOrganizationRequest(
    String name,
    String billingEmail,
    Address billingAddress,
    OrganizationSettings settings,
    String taxId,
    String ktp
) {}
