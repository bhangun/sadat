package tech.kayys.wayang.schema;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * PointDTO - 2D point
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PointDTO {
    private double x;
    private double y;

    public PointDTO() {
    }

    public PointDTO(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
}
