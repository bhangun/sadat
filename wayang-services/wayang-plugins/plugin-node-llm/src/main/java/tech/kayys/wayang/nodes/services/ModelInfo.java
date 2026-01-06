
/**
 * Model info
 */
public class ModelInfo {
    private String id;
    private int maxTokens;
    private boolean supportsFunction;
    private boolean available;
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ModelInfo model = new ModelInfo();
        
        public Builder id(String id) {
            model.id = id;
            return this;
        }
        
        public Builder maxTokens(int maxTokens) {
            model.maxTokens = maxTokens;
            return this;
        }
        
        public Builder supportsFunction(boolean supportsFunction) {
            model.supportsFunction = supportsFunction;
            return this;
        }
        
        public Builder available(boolean available) {
            model.available = available;
            return this;
        }
        
        public ModelInfo build() {
            return model;
        }
    }
    
    // Getters
    public String getId() { return id; }
    public int getMaxTokens() { return maxTokens; }
    public boolean supportsFunction() { return supportsFunction; }
    public boolean isAvailable() { return available; }
}