
public class AuditEntry {
    private Object event;
    private AuditLevel level;
    private List<String> tags;
    private String runId;
    private String nodeId;
    private String tenantId;
    private Instant timestamp;
    private Map<String, Object> metadata;
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private AuditEntry entry = new AuditEntry();
        
        public Builder event(Object event) {
            entry.event = event;
            return this;
        }
        
        public Builder level(AuditLevel level) {
            entry.level = level;
            return this;
        }
        
        public Builder tags(List<String> tags) {
            entry.tags = tags;
            return this;
        }
        
        public Builder runId(String runId) {
            entry.runId = runId;
            return this;
        }
        
        public Builder nodeId(String nodeId) {
            entry.nodeId = nodeId;
            return this;
        }
        
        public Builder tenantId(String tenantId) {
            entry.tenantId = tenantId;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            entry.timestamp = timestamp;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            entry.metadata = metadata;
            return this;
        }
        
        public AuditEntry build() {
            return entry;
        }
    }
    
    // Getters
    public Object getEvent() { return event; }
    public AuditLevel getLevel() { return level; }
    public List<String> getTags() { return tags; }
    public String getRunId() { return runId; }
    public String getNodeId() { return nodeId; }
    public String getTenantId() { return tenantId; }
    public Instant getTimestamp() { return timestamp; }
    public Map<String, Object> getMetadata() { return metadata; }
}
