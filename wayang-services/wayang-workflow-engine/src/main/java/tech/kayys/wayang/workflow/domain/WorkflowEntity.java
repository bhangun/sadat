package tech.kayys.wayang.workflow.domain;

import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Workflow entity for persistence.
 */
@Entity
@Table(name = "workflows")
public class WorkflowEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String version;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> definition;

    private java.time.Instant createdAt;

    private String createdBy;

    private String status;

    public static WorkflowEntity fromDefinition(WorkflowDefinition def, String tenantId) {
        WorkflowEntity entity = new WorkflowEntity();
        entity.setId(def.getId().getValue());
        entity.setTenantId(tenantId);
        entity.setName(def.getName());
        entity.setVersion(def.getVersion());
        entity.setCreatedAt(java.time.Instant.now());
        return entity;
    }

    public WorkflowDefinition toDefinition() {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setId(this.id);
        def.setName(this.name);
        def.setVersion(this.version);
        return def;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, Object> getDefinition() {
        return definition;
    }

    public void setDefinition(Map<String, Object> definition) {
        this.definition = definition;
    }

    public java.time.Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
