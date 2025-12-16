package tech.kayys.wayang.common;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * AuditEvent - Audit event structure
 */
public record AuditEvent(
        String id,
        String event,
        String actor,
        String tenantId,
        String targetType,
        String targetId,
        Instant timestamp,
        Map<String, Object> metadata) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id = UUID.randomUUID().toString();
        private String event;
        private String actor;
        private String tenantId;
        private String targetType = "workflow";
        private String targetId;
        private Instant timestamp = Instant.now();
        private Map<String, Object> metadata = Map.of();

        public Builder event(String event) {
            this.event = event;
            return this;
        }

        public Builder actor(String actor) {
            this.actor = actor;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder targetType(String targetType) {
            this.targetType = targetType;
            return this;
        }

        public Builder targetId(String targetId) {
            this.targetId = targetId;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public AuditEvent build() {
            return new AuditEvent(
                    id, event, actor, tenantId, targetType, targetId, timestamp, metadata);
        }
    }
}
