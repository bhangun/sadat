package tech.kayys.silat.api.subworkflow;

import jakarta.validation.constraints.NotNull;
import java.util.List;

record GrantCrossTenantPermissionRequest(
    @NotNull String targetTenantId,
    @NotNull List<String> permissions
) {}