package tech.kayys.wayang.workflow.security.model;

import lombok.Builder;
import lombok.NonNull;

/**
 * Structured permission model for fine-grained authorization.
 * Format: resource:action:target
 * Example: integration:call:sap-prod
 */
@Builder
public record Permission(
        @NonNull String resource,
        @NonNull String action,
        @NonNull String target) {

    public static final String WILDCARD = "*";

    @Override
    public String toString() {
        return String.format("%s:%s:%s", resource, action, target);
    }

    public boolean implies(Permission other) {
        if (other == null)
            return false;

        return checkPart(this.resource, other.resource) &&
                checkPart(this.action, other.action) &&
                checkPart(this.target, other.target);
    }

    private boolean checkPart(String valid, String required) {
        return WILDCARD.equals(valid) || valid.equals(required);
    }

    public static Permission fromString(String permissionString) {
        String[] parts = permissionString.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException(
                    "Permission string must be customizable in format resource:action:target");
        }
        return new Permission(parts[0], parts[1], parts[2]);
    }
}
