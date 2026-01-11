package tech.kayys.wayang.billing.model;

import java.time.Instant;
import java.util.Map;

/**
 * ============================================================================
 * SILAT EVENT SYSTEM & NOTIFICATIONS
 * ============================================================================
 * 
 * Event-driven architecture with:
 * - Domain events publishing (Kafka)
 * - Event-driven workflows
 * - Multi-channel notifications (Email, Slack, Webhooks)
 * - Audit logging
 * - Real-time updates (WebSocket)
 */

// ==================== DOMAIN EVENTS ====================

/**
 * Base domain event
 */
public interface DomainEvent {
    String eventId();
    String eventType();
    Instant occurredAt();
    String tenantId();
    Map<String, Object> metadata();
}
