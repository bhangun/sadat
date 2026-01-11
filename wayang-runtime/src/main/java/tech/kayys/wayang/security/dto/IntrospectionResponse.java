package tech.kayys.wayang.security.dto;

import java.util.Set;

public record IntrospectionResponse(
        boolean active,
        String consumerId,
        String tenantId,
        String workspaceId,
        String planId,
        Set<String> scopes) {
}
