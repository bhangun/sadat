package tech.kayys.wayang.organization.model;

/**
 * Organization status lifecycle
 */
public enum OrganizationStatus {
    PENDING,         // Registration pending
    ACTIVE,          // Active and operational
    SUSPENDED,       // Temporarily suspended
    DELINQUENT,      // Payment overdue
    DELETED,         // Soft deleted
    ARCHIVED         // Archived (inactive)
}
