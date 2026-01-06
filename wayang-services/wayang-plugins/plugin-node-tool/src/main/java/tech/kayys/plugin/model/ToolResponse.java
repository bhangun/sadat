
public class ToolResponse {
    private Object result;
    private long duration;
    private String error;
    private boolean retryable;
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static ToolResponse error(String error) {
        var response = new ToolResponse();
        response.error = error;
        response.retryable = false;
        return response;
    }
    
    public static class Builder {
        private ToolResponse response = new ToolResponse();
        
        public Builder result(Object result) {
            response.result = result;
            return this;
        }
        
        public Builder duration(long duration) {
            response.duration = duration;
            return this;
        }
        
        public Builder error(String error) {
            response.error = error;
            return this;
        }
        
        public Builder retryable(boolean retryable) {
            response.retryable = retryable;
            return this;
        }
        
        public ToolResponse build() {
            return response;
        }
    }
    
    // Getters and setters
    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }
    
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public boolean isRetryable() { return retryable; }
    public void setRetryable(boolean retryable) { this.retryable = retryable; }
    
    public boolean isError() { return error != null; }
}