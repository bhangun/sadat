package tech.kayys.wayang.memory.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MemoryConfig {

    private MemoryType type;
    private StorageBackend storageBackend;
    private Map<String, Object> config;
    private Retention retention;

    public enum MemoryType {
        @JsonProperty("none")
        NONE,
        @JsonProperty("buffer")
        BUFFER,
        @JsonProperty("window")
        WINDOW,
        @JsonProperty("summary")
        SUMMARY,
        @JsonProperty("vector")
        VECTOR,
        @JsonProperty("entity")
        ENTITY,
        @JsonProperty("knowledge_graph")
        KNOWLEDGE_GRAPH
    }

    public enum StorageBackend {
        @JsonProperty("in_memory")
        IN_MEMORY,
        @JsonProperty("sqlite")
        SQLITE,
        @JsonProperty("postgres")
        POSTGRES,
        @JsonProperty("redis")
        REDIS,
        @JsonProperty("mongodb")
        MONGODB,
        @JsonProperty("pinecone")
        PINECONE,
        @JsonProperty("weaviate")
        WEAVIATE,
        @JsonProperty("custom")
        CUSTOM
    }

    public static class Retention {
        private Integer duration;
        private String unit;
        private Boolean autoCleanup;

        // Getters and Setters
        public Integer getDuration() {
            return duration;
        }

        public void setDuration(Integer duration) {
            this.duration = duration;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public Boolean getAutoCleanup() {
            return autoCleanup;
        }

        public void setAutoCleanup(Boolean autoCleanup) {
            this.autoCleanup = autoCleanup;
        }
    }

    // Getters and Setters
    public MemoryType getType() {
        return type;
    }

    public void setType(MemoryType type) {
        this.type = type;
    }

    public StorageBackend getStorageBackend() {
        return storageBackend;
    }

    public void setStorageBackend(StorageBackend storageBackend) {
        this.storageBackend = storageBackend;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public Retention getRetention() {
        return retention;
    }

    public void setRetention(Retention retention) {
        this.retention = retention;
    }
}