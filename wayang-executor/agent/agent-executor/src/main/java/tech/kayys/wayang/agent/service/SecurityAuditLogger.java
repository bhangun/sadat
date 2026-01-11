package tech.kayys.wayang.agent.service;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.dto.AuditLogEntry;
import tech.kayys.wayang.agent.repository.AuditLogRepository;

/**
 * Audit logging for security events
 */
@ApplicationScoped
public class SecurityAuditLogger {

        private static final Logger LOG = LoggerFactory.getLogger(SecurityAuditLogger.class);

        @Inject
        AuditLogRepository auditLogRepository;

        /**
         * Log authentication event
         */
        public void logAuthentication(
                        String userId,
                        String tenantId,
                        boolean success,
                        String ipAddress) {

                AuditLogEntry entry = new AuditLogEntry(
                                "AUTHENTICATION",
                                userId,
                                tenantId,
                                success ? "SUCCESS" : "FAILURE",
                                Map.of("ipAddress", ipAddress),
                                Instant.now());

                persistAuditLog(entry);
        }

        /**
         * Log authorization event
         */
        public void logAuthorization(
                        String userId,
                        String tenantId,
                        String resource,
                        String action,
                        boolean granted) {

                AuditLogEntry entry = new AuditLogEntry(
                                "AUTHORIZATION",
                                userId,
                                tenantId,
                                granted ? "GRANTED" : "DENIED",
                                Map.of("resource", resource, "action", action),
                                Instant.now());

                persistAuditLog(entry);
        }

        /**
         * Log data access
         */
        public void logDataAccess(
                        String userId,
                        String tenantId,
                        String resourceType,
                        String resourceId,
                        String action) {

                AuditLogEntry entry = new AuditLogEntry(
                                "DATA_ACCESS",
                                userId,
                                tenantId,
                                action,
                                Map.of("resourceType", resourceType, "resourceId", resourceId),
                                Instant.now());

                persistAuditLog(entry);
        }

        private void persistAuditLog(AuditLogEntry entry) {
                auditLogRepository.save(entry)
                                .subscribe().with(
                                                v -> LOG.trace("Audit log saved: {}", entry.eventType()),
                                                error -> LOG.error("Failed to save audit log", error));
        }
}
