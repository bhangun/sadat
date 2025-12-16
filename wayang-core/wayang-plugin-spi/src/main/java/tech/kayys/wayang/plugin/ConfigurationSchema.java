package tech.kayys.wayang.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Configuration Schema
 */
public class ConfigurationSchema {
    

    private Map<String, PropertySchema> properties;
    

    private List<String> required = new ArrayList<>();
    
    private Map<String, Object> defaults;


    public Map<String, PropertySchema> getProperties() {
        return this.properties;
    }

    public void setProperties(Map<String, PropertySchema> properties) {
        this.properties = properties;
    }

    public List<String> getRequired() {
        return this.required;
    }

    public void setRequired(List<String> required) {
        this.required = required;
    }

    public Map<String, Object> getDefaults() {
        return this.defaults;
    }

    public void setDefaults(Map<String, Object> defaults) {
        this.defaults = defaults;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<String, PropertySchema> properties;
        private List<String> required = new ArrayList<>();
        private Map<String, Object> defaults;

        public Builder properties(Map<String, PropertySchema> properties) {
            this.properties = properties;
            return this;
        }

        public Builder required(List<String> required) {
            this.required = required;
            return this;
        }

        public Builder defaults(Map<String, Object> defaults) {
            this.defaults = defaults;
            return this;
        }

        public ConfigurationSchema build() {
            ConfigurationSchema schema = new ConfigurationSchema();
            schema.setProperties(this.properties);
            schema.setRequired(this.required);
            schema.setDefaults(this.defaults);
            return schema;
        }
    }
}
