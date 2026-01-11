package tech.kayys.wayang.billing.model;

public enum UserRole {
    OWNER,          // Organization owner (full access)
    ADMIN,          // Administrator (most access)
    MEMBER,         // Regular member
    BILLING,        // Billing only access
    VIEWER          // Read-only access
}