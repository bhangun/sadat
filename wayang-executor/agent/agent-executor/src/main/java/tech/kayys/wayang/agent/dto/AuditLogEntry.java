package tech.kayys.wayang.agent.dto;

import java.time.Instant;
import java.util.Map;

public record AuditLogEntry(
                String eventType,
                String userId,
                String tenantId,
                String outcome,
                Map<String, String> details,
                Instant timestamp) {
}