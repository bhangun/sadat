package tech.kayys.wayang.schema;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * ValidationStatus - Validation status
 */
@RegisterForReflection
public enum ValidationStatus {
    NOT_VALIDATED,
    VALIDATING,
    VALID,
    INVALID,
    NEEDS_REVIEW
}
