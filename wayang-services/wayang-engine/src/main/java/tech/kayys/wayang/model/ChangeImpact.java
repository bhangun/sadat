package tech.kayys.wayang.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * ChangeImpact - Impact level of changes
 */
@RegisterForReflection
public enum ChangeImpact {
    NONE, // No functional changes
    PATCH, // Minor UI/metadata changes
    MINOR, // New features, backwards compatible
    MAJOR, // Modifications to existing functionality
    BREAKING, // Breaking changes (deletions, incompatible modifications)
    UNKNOWN // Impact cannot be determined
}
