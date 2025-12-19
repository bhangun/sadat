package tech.kayys.wayang.schema;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * NodeUIDTO - Node UI properties
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeUIDTO {

    @NotBlank
    private String ref; // Node ID reference

    @NotNull
    private PointDTO position;

    private SizeDTO size;
    private String icon;
    private String color;
    private String shape;
    private boolean collapsed = false;
    private int zIndex = 0;

    // Getters and setters...
    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public PointDTO getPosition() {
        return position;
    }

    public void setPosition(PointDTO position) {
        this.position = position;
    }

    public SizeDTO getSize() {
        return size;
    }

    public void setSize(SizeDTO size) {
        this.size = size;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
