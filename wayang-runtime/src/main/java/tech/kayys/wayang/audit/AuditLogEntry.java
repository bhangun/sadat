package tech.kayys.wayang.audit;

import java.util.Map;

public record AuditLogEntry(
                String tenantId,
                String userId,
                String action,
                String resourceType,
                String resourceId,
                String result,
                String ipAddress,
                String userAgent,
                Map<String, Object> details) {
}