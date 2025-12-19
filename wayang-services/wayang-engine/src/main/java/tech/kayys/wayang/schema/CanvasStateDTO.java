package tech.kayys.wayang.schema;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * CanvasStateDTO - Canvas state
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CanvasStateDTO {
    private double zoom = 1.0;
    private PointDTO offset = new PointDTO(0, 0);
    private String background;
    private boolean snapToGrid = true;

    // Getters and setters...
    public double getZoom() {
        return zoom;
    }

    public void setZoom(double zoom) {
        this.zoom = zoom;
    }

    public PointDTO getOffset() {
        return offset;
    }

    public void setOffset(PointDTO offset) {
        this.offset = offset;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public boolean isSnapToGrid() {
        return snapToGrid;
    }

    public void setSnapToGrid(boolean snapToGrid) {
        this.snapToGrid = snapToGrid;
    }
}
