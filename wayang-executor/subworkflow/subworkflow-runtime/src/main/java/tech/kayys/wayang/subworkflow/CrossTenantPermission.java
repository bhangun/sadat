package tech.kayys.silat.api.subworkflow;

import java.time.Instant;
import java.util.List;

record CrossTenantPermission(
    String permissionId,
    String sourceTenantId,
    String targetTenantId,
    List<String> permissions,
    Instant grantedAt,
    Instant expiresAt
) {}