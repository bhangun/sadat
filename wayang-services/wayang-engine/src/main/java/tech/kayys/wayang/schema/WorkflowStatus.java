package tech.kayys.wayang.schema;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * WorkflowStatus - Workflow status
 */
@RegisterForReflection
public enum WorkflowStatus {
    DRAFT,
    VALIDATING,
    VALID,
    INVALID,
    PUBLISHED,
    ARCHIVED
}
