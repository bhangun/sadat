package tech.kayys.wayang.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.Map;

@RegisterForReflection
public class AuditEvent {
    private String type;
    private String entityType;
    private String entityId;
    private String userId;
    private String tenantId;
    private Map<String, Object> metadata;
    private Map<String, Object> changes;

    public AuditEvent() {
    }

    public static Builder builder() {
        return new Builder();
    }

    private AuditEvent(Builder builder) {
        this.type = builder.type;
        this.entityType = builder.entityType;
        this.entityId = builder.entityId;
        this.userId = builder.userId;
        this.tenantId = builder.tenantId;
        this.metadata = builder.metadata;
        this.changes = builder.changes;
    }

    public static class Builder {
        private String type;
        private String entityType;
        private String entityId;
        private String userId;
        private String tenantId;
        private Map<String, Object> metadata;
        private Map<String, Object> changes;

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder entityType(String entityType) {
            this.entityType = entityType;
            return this;
        }

        public Builder entityId(String entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder changes(Map<String, Object> changes) {
            this.changes = changes;
            return this;
        }

        public AuditEvent build() {
            return new AuditEvent(this);
        }
    }

    // Getters
    public String getType() {
        return type;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getUserId() {
        return userId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Map<String, Object> getChanges() {
        return changes;
    }
}
