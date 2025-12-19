package tech.kayys.agent.schema;

import java.util.List;

public record SecurityPolicy(
    List<String> requiredPermissions,
    String sandbox,
    List<BusinessRule> businessRules
) {
    public SecurityPolicy {
        requiredPermissions = List.copyOf(requiredPermissions);
        businessRules = List.copyOf(businessRules);
        if (sandbox == null) sandbox = "restricted";
    }

    public static SecurityPolicy defaultPolicy() {
        return new SecurityPolicy(List.of(), "restricted", List.of());
    }
}