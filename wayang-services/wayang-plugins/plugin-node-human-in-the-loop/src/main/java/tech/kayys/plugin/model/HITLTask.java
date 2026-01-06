
public class HITLTask {
    private String type;
    private Map<String, Object> data;
    private String runId;
    private String nodeId;
    private String tenantId;
    private int timeout;
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private HITLTask task = new HITLTask();
        
        public Builder type(String type) {
            task.type = type;
            return this;
        }
        
        public Builder data(Map<String, Object> data) {
            task.data = data;
            return this;
        }
        
        public Builder runId(String runId) {
            task.runId = runId;
            return this;
        }
        
        public Builder nodeId(String nodeId) {
            task.nodeId = nodeId;
            return this;
        }
        
        public Builder tenantId(String tenantId) {
            task.tenantId = tenantId;
            return this;
        }
        
        public Builder timeout(int timeout) {
            task.timeout = timeout;
            return this;
        }
        
        public HITLTask build() {
            return task;
        }
    }
    
    // Getters
    public String getType() { return type; }
    public Map<String, Object> getData() { return data; }
    public String getRunId() { return runId; }
    public String getNodeId() { return nodeId; }
    public String getTenantId() { return tenantId; }
    public int getTimeout() { return timeout; }
}