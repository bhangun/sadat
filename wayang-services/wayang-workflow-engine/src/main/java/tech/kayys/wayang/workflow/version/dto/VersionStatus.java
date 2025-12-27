package tech.kayys.wayang.workflow.version.dto;

public enum VersionStatus {
    DRAFT, // Created but not published
    CANARY, // In canary deployment
    PUBLISHED, // Fully deployed
    DEPRECATED, // Marked for removal
    ARCHIVED, // No longer active
    ROLLED_BACK // Rolled back due to issues
}
