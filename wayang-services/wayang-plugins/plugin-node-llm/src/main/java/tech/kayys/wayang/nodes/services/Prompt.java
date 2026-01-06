package tech.kayys.wayang.nodes.services;

import java.util.List;


public class Prompt {
    private String system;
    private String user;
    private Map<String, Object> context;
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Prompt prompt = new Prompt();
        
        public Builder system(String system) {
            prompt.system = system;
            return this;
        }
        
        public Builder user(String user) {
            prompt.user = user;
            return this;
        }
        
        public Builder context(Map<String, Object> context) {
            prompt.context = context;
            return this;
        }
        
        public Prompt build() {
            return prompt;
        }
    }
    
    public List<Map<String, String>> toMessages() {
        var messages = new ArrayList<Map<String, String>>();
        
        if (system != null) {
            messages.add(Map.of("role", "system", "content", system));
        }
        
        if (user != null) {
            messages.add(Map.of("role", "user", "content", user));
        }
        
        return messages;
    }
    
    // Getters
    public String getSystem() { return system; }
    public String getUser() { return user; }
    public Map<String, Object> getContext() { return context; }
}