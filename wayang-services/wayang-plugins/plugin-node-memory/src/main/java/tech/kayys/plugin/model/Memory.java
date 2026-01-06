
public class Memory {
    private String id;
    private MemoryType type;
    private Object content;
    private Duration ttl;
    private String runId;
    private String tenantId;
    private Map<String, Object> metadata;
    private Instant createdAt;
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Memory memory = new Memory();
        
        public Builder id(String id) {
            memory.id = id;
            return this;
        }
        
        public Builder type(MemoryType type) {
            memory.type = type;
            return this;
        }
        
        public Builder content(Object content) {
            memory.content = content;
            return this;
        }
        
        public Builder ttl(Duration ttl) {
            memory.ttl = ttl;
            return this;
        }
        
        public Builder runId(String runId) {
            memory.runId = runId;
            return this;
        }
        
        public Builder tenantId(String tenantId) {
            memory.tenantId = tenantId;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            memory.metadata = metadata;
            return this;
        }
        
        public Builder createdAt(Instant createdAt) {
            memory.createdAt = createdAt;
            return this;
        }
        
        public Memory build() {
            return memory;
        }
    }
    
    // Getters
    public String getId() { return id; }
    public MemoryType getType() { return type; }
    public Object getContent() { return content; }
    public Duration getTtl() { return ttl; }
    public String getRunId() { return runId; }
    public String getTenantId() { return tenantId; }
    public Map<String, Object> getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
}