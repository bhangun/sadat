
public class Persona {
    private String id;
    private String name;
    private String systemPrompt;
    private Map<String, Object> config;
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Persona persona = new Persona();
        
        public Builder id(String id) {
            persona.id = id;
            return this;
        }
        
        public Builder name(String name) {
            persona.name = name;
            return this;
        }
        
        public Builder systemPrompt(String systemPrompt) {
            persona.systemPrompt = systemPrompt;
            return this;
        }
        
        public Builder config(Map<String, Object> config) {
            persona.config = config;
            return this;
        }
        
        public Persona build() {
            return persona;
        }
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getSystemPrompt() { return systemPrompt; }
    public Map<String, Object> getConfig() { return config; }
}
