package tech.kayys.wayang.workflow.service.backup;

import java.time.Instant;
import java.util.Map;

/**
 * System configuration entity
 */
public class SystemConfig {
    private String id;
    private String key;
    private String value;
    private Instant modifiedAt;
    private Map<String, Object> metadata;

    public SystemConfig() {
    }

    public SystemConfig(String id, String key, String value) {
        this.id = id;
        this.key = key;
        this.value = value;
        this.modifiedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Instant getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Instant modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
