package tech.kayys.wayang.hitl.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * TaskAuditEntry - Audit trail entry
 */
record TaskAuditEntry(
    String entryId,
    String action,
    String details,
    String performedBy,
    Instant timestamp
) {}