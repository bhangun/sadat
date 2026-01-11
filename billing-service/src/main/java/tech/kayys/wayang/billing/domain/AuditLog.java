package tech.kayys.wayang.billing.domain;


import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import tech.kayys.wayang.billing.model.AuditActionType;
import tech.kayys.wayang.billing.model.AuditSeverity;

import java.time.Instant;
import java.util.*;

/**
 * ============================================================================
 * SILAT ADMIN DASHBOARD & ANALYTICS
 * ============================================================================
 * 
 * Comprehensive admin interface with:
 * - Platform-wide analytics
 * - Revenue tracking & forecasting
 * - Tenant health monitoring
 * - Usage analytics
 * - Churn analysis
 * - Performance metrics
 */

// ==================== AUDIT LOGGING ====================

/**
 * Audit log entity
 */
@Entity
@Table(name = "mgmt_audit_logs", indexes = {
    @Index(name = "idx_audit_tenant", columnList = "tenant_id"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_action", columnList = "action_type"),
    @Index(name = "idx_audit_user", columnList = "user_id")
})
public class AuditLog extends PanacheEntityBase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "audit_id")
    public UUID auditId;
    
    @Column(name = "tenant_id")
    public String tenantId;
    
    @Column(name = "user_id")
    public String userId;
    
    @Column(name = "user_email")
    public String userEmail;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type")
    public AuditActionType actionType;
    
    @Column(name = "resource_type")
    public String resourceType;
    
    @Column(name = "resource_id")
    public String resourceId;
    
    @Column(name = "action")
    public String action;
    
    @Column(name = "description")
    public String description;
    
    @Column(name = "ip_address")
    public String ipAddress;
    
    @Column(name = "user_agent")
    public String userAgent;
    
    @Column(name = "request_id")
    public String requestId;
    
    @Column(name = "timestamp")
    public Instant timestamp;
    
    @Column(name = "before_state", columnDefinition = "jsonb")
    public Map<String, Object> beforeState;
    
    @Column(name = "after_state", columnDefinition = "jsonb")
    public Map<String, Object> afterState;
    
    @Column(name = "metadata", columnDefinition = "jsonb")
    public Map<String, Object> metadata;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    public AuditSeverity severity = AuditSeverity.INFO;
    
    @Column(name = "success")
    public boolean success = true;
    
    @Column(name = "error_message")
    public String errorMessage;
}
