package tech.kayys.wayang.audit;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.security.domain.SecurityAuditLog;

/**
 * Audit logging service
 */
@ApplicationScoped
public class AuditLogger {

    private static final Logger LOG = LoggerFactory.getLogger(AuditLogger.class);

    public Uni<Void> log(AuditLogEntry entry) {
        LOG.info("Audit: {} - {} on {} by {}",
                entry.result(), entry.action(), entry.resourceType(), entry.userId());

        return io.quarkus.hibernate.reactive.panache.Panache.withTransaction(() -> {
            SecurityAuditLog log = new SecurityAuditLog();
            log.tenantId = entry.tenantId();
            log.userId = entry.userId();
            log.action = entry.action();
            log.resourceType = entry.resourceType();
            log.resourceId = entry.resourceId();
            log.result = entry.result();
            log.ipAddress = entry.ipAddress();
            log.userAgent = entry.userAgent();
            log.timestamp = Instant.now();
            log.details = entry.details();

            return log.persist().replaceWithVoid();
        });
    }
}
