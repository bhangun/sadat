
public class ToolRequest {
    private String tool;
    private Object parameters;
    private int timeout;
    private String runId;
    private String tenantId;
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ToolRequest request = new ToolRequest();
        
        public Builder tool(String tool) {
            request.tool = tool;
            return this;
        }
        
        public Builder parameters(Object parameters) {
            request.parameters = parameters;
            return this;
        }
        
        public Builder timeout(int timeout) {
            request.timeout = timeout;
            return this;
        }
        
        public Builder runId(String runId) {
            request.runId = runId;
            return this;
        }
        
        public Builder tenantId(String tenantId) {
            request.tenantId = tenantId;
            return this;
        }
        
        public ToolRequest build() {
            return request;
        }
    }
    
    // Getters
    public String getTool() { return tool; }
    public Object getParameters() { return parameters; }
    public int getTimeout() { return timeout; }
    public String getRunId() { return runId; }
    public String getTenantId() { return tenantId; }
}
