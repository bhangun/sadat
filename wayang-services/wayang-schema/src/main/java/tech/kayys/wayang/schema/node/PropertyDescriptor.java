package tech.kayys.wayang.schema.node;

public class PropertyDescriptor {
    private String name;
    private String displayName;
    private String description;
    private String type;
    private Object defaultValue;
    private boolean required = false;
    private PropertyValidation validation;
    private boolean sensitive = false;

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getDefault() {
        return defaultValue;
    }

    public void setDefault(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public PropertyValidation getValidation() {
        return validation;
    }

    public void setValidation(PropertyValidation validation) {
        this.validation = validation;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public void setSensitive(boolean sensitive) {
        this.sensitive = sensitive;
    }
}