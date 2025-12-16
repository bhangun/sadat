package tech.kayys.wayang.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Variable {

    private String id;
    private String name;
    private VariableType type;
    private Object defaultValue;
    private Scope scope;
    private Boolean persistent;
    private Boolean encrypted;
    private Validation validation;

    public enum VariableType {
        @JsonProperty("string")
        STRING,
        @JsonProperty("number")
        NUMBER,
        @JsonProperty("boolean")
        BOOLEAN,
        @JsonProperty("array")
        ARRAY,
        @JsonProperty("object")
        OBJECT,
        @JsonProperty("null")
        NULL,
        @JsonProperty("any")
        ANY
    }

    public enum Scope {
        @JsonProperty("global")
        GLOBAL,
        @JsonProperty("agent")
        AGENT,
        @JsonProperty("workflow")
        WORKFLOW,
        @JsonProperty("node")
        NODE,
        @JsonProperty("local")
        LOCAL
    }

    public static class Validation {
        private Boolean required;
        private Double min;
        private Double max;
        private String pattern;
        private String custom;

        // Getters and Setters
        public Boolean getRequired() {
            return required;
        }

        public void setRequired(Boolean required) {
            this.required = required;
        }

        public Double getMin() {
            return min;
        }

        public void setMin(Double min) {
            this.min = min;
        }

        public Double getMax() {
            return max;
        }

        public void setMax(Double max) {
            this.max = max;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public String getCustom() {
            return custom;
        }

        public void setCustom(String custom) {
            this.custom = custom;
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VariableType getType() {
        return type;
    }

    public void setType(VariableType type) {
        this.type = type;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public Boolean getPersistent() {
        return persistent;
    }

    public void setPersistent(Boolean persistent) {
        this.persistent = persistent;
    }

    public Boolean getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
    }

    public Validation getValidation() {
        return validation;
    }

    public void setValidation(Validation validation) {
        this.validation = validation;
    }
}
