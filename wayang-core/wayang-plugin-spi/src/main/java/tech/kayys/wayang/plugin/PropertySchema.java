package tech.kayys.wayang.plugin;

import java.util.List;
import java.util.Map;

/**
 * Property Schema
 */
public class PropertySchema {
    

    private String type; // string, number, boolean, object, array
    
    private String description;
    private Object defaultValue;
    
    // Validation
    private Number minimum;
    private Number maximum;
    private String pattern;
    private List<Object> enumValues;
    
    // For nested objects
    private Map<String, PropertySchema> properties;
    
    // For arrays
    private PropertySchema items;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Number getMinimum() {
        return minimum;
    }

    public void setMinimum(Number minimum) {
        this.minimum = minimum;
    }

    public Number getMaximum() {
        return maximum;
    }

    public void setMaximum(Number maximum) {
        this.maximum = maximum;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public List<Object> getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(List<Object> enumValues) {
        this.enumValues = enumValues;
    }

    public Map<String, PropertySchema> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, PropertySchema> properties) {
        this.properties = properties;
    }

    public PropertySchema getItems() {
        return items;
    }

    public void setItems(PropertySchema items) {
        this.items = items;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String type;
        private String description;
        private Object defaultValue;
        private Number minimum;
        private Number maximum;
        private String pattern;
        private List<Object> enumValues;
        private Map<String, PropertySchema> properties;
        private PropertySchema items;

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder minimum(Number minimum) {
            this.minimum = minimum;
            return this;
        }

        public Builder maximum(Number maximum) {
            this.maximum = maximum;
            return this;
        }

        public Builder pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        public Builder enumValues(List<Object> enumValues) {
            this.enumValues = enumValues;
            return this;
        }

        public Builder properties(Map<String, PropertySchema> properties) {
            this.properties = properties;
            return this;
        }

        public Builder items(PropertySchema items) {
            this.items = items;
            return this;
        }

        public PropertySchema build() {
            PropertySchema schema = new PropertySchema();
            schema.type = this.type;
            schema.description = this.description;
            schema.defaultValue = this.defaultValue;
            schema.minimum = this.minimum;
            schema.maximum = this.maximum;
            schema.pattern = this.pattern;
            schema.enumValues = this.enumValues;
            schema.properties = this.properties;
            schema.items = this.items;
            return schema;
        }
    }
}