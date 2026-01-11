package tech.kayys.wayang.billing.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AddAddonRequest(
    @NotNull UUID addonCatalogId,
    int quantity
) {}

