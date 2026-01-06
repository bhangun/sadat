package tech.kayys.wayang.nodes.services;

import java.util.List;
import java.util.Map;


public class LLMRequest {
    private Prompt prompt;
    private Map<String, Object> modelHints;
    private List<Map<String, Object>> functions;
    private int maxTokens;
    private double temperature;
    private boolean stream;
    private Map<String, Object> metadata;
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private LLMRequest request = new LLMRequest();
        
        public Builder prompt(Prompt prompt) {
            request.prompt = prompt;
            return this;
        }
        
        public Builder modelHints(Map<String, Object> hints) {
            request.modelHints = hints;
            return this;
        }
        
        public Builder functions(List<Map<String, Object>> functions) {
            request.functions = functions;
            return this;
        }
        
        public Builder maxTokens(int maxTokens) {
            request.maxTokens = maxTokens;
            return this;
        }
        
        public Builder temperature(double temperature) {
            request.temperature = temperature;
            return this;
        }
        
        public Builder stream(boolean stream) {
            request.stream = stream;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            request.metadata = metadata;
            return this;
        }
        
        public LLMRequest build() {
            return request;
        }
    }
    
    // Getters
    public Prompt getPrompt() { return prompt; }
    public Map<String, Object> getModelHints() { return modelHints; }
    public List<Map<String, Object>> getFunctions() { return functions; }
    public int getMaxTokens() { return maxTokens; }
    public double getTemperature() { return temperature; }
    public boolean isStream() { return stream; }
    public Map<String, Object> getMetadata() { return metadata; }
}