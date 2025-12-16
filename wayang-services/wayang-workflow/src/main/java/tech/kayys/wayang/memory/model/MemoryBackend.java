package tech.kayys.wayang.memory.model;

import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.Map;

/**
 * Memory backend interface
 */
public interface MemoryBackend {
    Uni<Void> store(String sessionId, MemoryEntry entry);

    Uni<List<MemoryEntry>> retrieve(String sessionId, int limit);

    Uni<List<MemoryEntry>> search(String sessionId, String query, int limit);

    Uni<Void> clear(String sessionId);

    tech.kayys.wayang.memory.model.StorageBackend getSupportedBackend();

    class MemoryEntry {
        private String id;
        private String sessionId;
        private String role;
        private String content;
        private Map<String, Object> metadata;
        private long timestamp;
        private Double[] embedding;

        public MemoryEntry() {
        }

        public MemoryEntry(String sessionId, String role, String content) {
            this.id = java.util.UUID.randomUUID().toString();
            this.sessionId = sessionId;
            this.role = role;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public Double[] getEmbedding() {
            return embedding;
        }

        public void setEmbedding(Double[] embedding) {
            this.embedding = embedding;
        }
    }
}