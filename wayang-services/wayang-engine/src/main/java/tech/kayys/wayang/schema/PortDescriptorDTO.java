package tech.kayys.wayang.schema;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * PortDescriptorDTO - Input/Output port definition
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PortDescriptorDTO {

    @NotBlank
    private String name;

    private String displayName;
    private String description;

    @NotNull
    private DataTypeDTO data;

    private boolean required = true;
    private boolean sensitive = false;

    // Getters and setters...
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

    public DataTypeDTO getData() {
        return data;
    }

    public void setData(DataTypeDTO data) {
        this.data = data;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public void setSensitive(boolean sensitive) {
        this.sensitive = sensitive;
    }
}
