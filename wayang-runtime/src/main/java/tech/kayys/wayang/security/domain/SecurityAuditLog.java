package tech.kayys.wayang.security.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Security audit log entry
 */
@Entity
@Table(name = "security_audit_log")
public class SecurityAuditLog extends io.quarkus.hibernate.reactive.panache.PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "tenant_id")
    public String tenantId;

    @Column(name = "user_id")
    public String userId;

    @Column(name = "action")
    public String action;

    @Column(name = "resource_type")
    public String resourceType;

    @Column(name = "resource_id")
    public String resourceId;

    @Column(name = "result")
    public String result; // SUCCESS, DENIED, ERROR

    @Column(name = "ip_address")
    public String ipAddress;

    @Column(name = "user_agent")
    public String userAgent;

    @Column(name = "timestamp")
    public Instant timestamp;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    public Map<String, Object> details;
}
