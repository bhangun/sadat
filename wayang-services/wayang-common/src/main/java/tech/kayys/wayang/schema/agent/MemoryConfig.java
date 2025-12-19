package tech.kayys.wayang.schema.agent;

import java.util.Arrays;
import java.util.List;

// Memory Configuration
public class MemoryConfig {
    private String type = "none";
    private String namespace;
    private Integer ttlDays;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        List<String> validTypes = Arrays.asList("none", "kv", "vector", "hybrid");
        if (!validTypes.contains(type)) {
            throw new IllegalArgumentException("Invalid memory type: " + type);
        }
        this.type = type;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Integer getTtlDays() {
        return ttlDays;
    }

    public void setTtlDays(Integer ttlDays) {
        if (ttlDays != null && ttlDays < 0) {
            throw new IllegalArgumentException("TTL days cannot be negative");
        }
        this.ttlDays = ttlDays;
    }

}
