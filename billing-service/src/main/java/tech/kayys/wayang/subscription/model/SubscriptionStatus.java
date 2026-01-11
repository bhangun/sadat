package tech.kayys.wayang.subscription.model;

/**
 * Subscription status
 */
public enum SubscriptionStatus {
    TRIAL,           // In trial period
    ACTIVE,          // Active subscription
    PAST_DUE,        // Payment overdue
    CANCELLED,       // Cancelled
    EXPIRED,         // Expired
    PAUSED           // Temporarily paused
}