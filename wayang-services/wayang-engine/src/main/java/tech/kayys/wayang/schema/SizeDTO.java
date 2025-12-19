package tech.kayys.wayang.schema;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * SizeDTO - 2D size
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SizeDTO {
    private double width;
    private double height;

    public SizeDTO() {
    }

    public SizeDTO(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }
}
