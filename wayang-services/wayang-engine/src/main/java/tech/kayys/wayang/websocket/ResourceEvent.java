package tech.kayys.wayang.websocket;

import java.time.Instant;
import java.util.Map;

/**
 * ResourceEvent - Generic resource event message
 */
public class ResourceEvent {
    private ResourceEventType type;
    private String resourceId;
    private String tenantId;
    private String actorId;
    private Instant timestamp;
    private Map<String, Object> payload;

    public ResourceEvent() {
    }

    public ResourceEvent(ResourceEventType type, String resourceId, String tenantId, String actorId,
            Map<String, Object> payload) {
        this.type = type;
        this.resourceId = resourceId;
        this.tenantId = tenantId;
        this.actorId = actorId;
        this.timestamp = Instant.now();
        this.payload = payload;
    }

    public ResourceEventType getType() {
        return type;
    }

    public void setType(ResourceEventType type) {
        this.type = type;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
