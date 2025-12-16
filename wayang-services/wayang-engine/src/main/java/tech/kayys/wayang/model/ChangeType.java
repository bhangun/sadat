package tech.kayys.wayang.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * ChangeType - Type of change
 */
@RegisterForReflection
public enum ChangeType {
    ADDED,
    DELETED,
    MODIFIED
}
