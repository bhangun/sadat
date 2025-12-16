package tech.kayys.agent.schema;

import java.util.List;

public record SecurityContext(
    String ownerId,                   // who owns this agent?
    List<String> authorizedRoles,     // RBAC-style
    String executionSandbox,          // e.g., "restricted", "full"
    boolean auditLoggingEnabled
) {}

