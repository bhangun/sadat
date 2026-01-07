package tech.kayys.wayang.billing.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.billing.domain.AuditLog;
import tech.kayys.wayang.billing.model.AuditActionType;
import tech.kayys.wayang.billing.model.AuditSeverity;
import tech.kayys.wayang.organization.domain.Organization;

/**
 * Audit logger service
 */
@ApplicationScoped
public class AuditLogger {
    
    private static final Logger LOG = LoggerFactory.getLogger(AuditLogger.class);
    
    /**
     * Log organization created
     */
    public Uni<Void> logOrganizationCreated(Organization org) {
        return logAudit(
            org.tenantId,
            null,
            AuditActionType.ORGANIZATION_CREATED,
            "Organization",
            org.organizationId.toString(),
            "Organization created: " + org.name,
            null,
            Map.of(
                "name", org.name,
                "slug", org.slug,
                "orgType", org.orgType
            ),
            AuditSeverity.INFO
        );
    }
    
    /**
     * Log organization updated
     */
    public Uni<Void> logOrganizationUpdated(Organization org) {
        return logAudit(
            org.tenantId,
            null,
            AuditActionType.ORGANIZATION_UPDATED,
            "Organization",
            org.organizationId.toString(),
            "Organization updated",
            null,
            null,
            AuditSeverity.INFO
        );
    }
    
    /**
     * Log organization suspended
     */
    public Uni<Void> logOrganizationSuspended(Organization org, String reason) {
        return logAudit(
            org.tenantId,
            null,
            AuditActionType.ORGANIZATION_SUSPENDED,
            "Organization",
            org.organizationId.toString(),
            "Organization suspended: " + reason,
            null,
            null,
            AuditSeverity.WARNING
        );
    }
    
    /**
     * Log organization activated
     */
    public Uni<Void> logOrganizationActivated(Organization org) {
        return logAudit(
            org.tenantId,
            null,
            AuditActionType.ORGANIZATION_ACTIVATED,
            "Organization",
            org.organizationId.toString(),
            "Organization activated",
            null,
            null,
            AuditSeverity.INFO
        );
    }
    
    /**
     * Log organization deleted
     */
    public Uni<Void> logOrganizationDeleted(Organization org) {
        return logAudit(
            org.tenantId,
            null,
            AuditActionType.ORGANIZATION_DELETED,
            "Organization",
            org.organizationId.toString(),
            "Organization deleted",
            null,
            null,
            AuditSeverity.WARNING
        );
    }
    
    /**
     * Generic event logger
     */
    public Uni<Void> logEvent(
            String tenantId,
            String actionType,
            String description) {
        
        return Panache.withTransaction(() -> {
            AuditLog log = new AuditLog();
            log.tenantId = tenantId;
            log.actionType = AuditActionType.valueOf(actionType);
            log.description = description;
            log.timestamp = Instant.now();
            log.severity = AuditSeverity.INFO;
            
            return log.persist()
                .replaceWithVoid();
        });
    }
    
    /**
     * Core audit logging method
     */
    private Uni<Void> logAudit(
            String tenantId,
            String userId,
            AuditActionType actionType,
            String resourceType,
            String resourceId,
            String description,
            Map<String, Object> beforeState,
            Map<String, Object> afterState,
            AuditSeverity severity) {
        
        return Panache.withTransaction(() -> {
            AuditLog log = new AuditLog();
            log.tenantId = tenantId;
            log.userId = userId;
            log.actionType = actionType;
            log.resourceType = resourceType;
            log.resourceId = resourceId;
            log.action = actionType.name();
            log.description = description;
            log.timestamp = Instant.now();
            log.beforeState = beforeState;
            log.afterState = afterState;
            log.severity = severity;
            log.metadata = new HashMap<>();
            
            return log.persist()
                .replaceWithVoid();
        })
        .onFailure().invoke(error ->
            LOG.error("Failed to log audit event", error)
        );
    }
    
    /**
     * Query audit logs
     */
    public Uni<List<AuditLog>> queryLogs(
            String tenantId,
            AuditActionType actionType,
            Instant from,
            Instant to,
            int page,
            int size) {
        
        StringBuilder query = new StringBuilder("tenantId = ?1");
        List<Object> params = new ArrayList<>();
        params.add(tenantId);
        
        if (actionType != null) {
            query.append(" and actionType = ?").append(params.size() + 1);
            params.add(actionType);
        }
        
        if (from != null) {
            query.append(" and timestamp >= ?").append(params.size() + 1);
            params.add(from);
        }
        
        if (to != null) {
            query.append(" and timestamp <= ?").append(params.size() + 1);
            params.add(to);
        }
        
        query.append(" order by timestamp desc");
        
        return AuditLog.find(query.toString(), params.toArray())
            .page(page, size)
            .list();
    }
}