package tech.kayys.wayang.schema;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;

/**
 * DataTypeDTO - Data type definition
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataTypeDTO {

    @NotBlank
    private String type; // json, string, number, etc.

    private String format;
    private Map<String, Object> schema; // JSON Schema
    private String multiplicity = "single"; // single, list, map, stream

    // Getters and setters...
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Map<String, Object> getSchema() {
        return schema;
    }

    public void setSchema(Map<String, Object> schema) {
        this.schema = schema;
    }

    public String getMultiplicity() {
        return multiplicity;
    }

    public void setMultiplicity(String multiplicity) {
        this.multiplicity = multiplicity;
    }
}