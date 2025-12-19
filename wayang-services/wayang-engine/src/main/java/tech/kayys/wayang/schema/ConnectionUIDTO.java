package tech.kayys.wayang.schema;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;

/**
 * ConnectionUIDTO - Connection UI properties
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectionUIDTO {

    @NotBlank
    private String ref; // Connection ID reference

    private String color;
    private String pathStyle;

    // Getters and setters...
    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getPathStyle() {
        return pathStyle;
    }

    public void setPathStyle(String pathStyle) {
        this.pathStyle = pathStyle;
    }
}
