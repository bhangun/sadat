package tech.kayys.wayang.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * CanvasDiff - Canvas state changes
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CanvasDiff {

    public boolean zoomChanged = false;
    public Double oldZoom;
    public Double newZoom;

    public boolean offsetChanged = false;
    public UIDefinition.Point oldOffset;
    public UIDefinition.Point newOffset;

    public boolean backgroundChanged = false;
    public String oldBackground;
    public String newBackground;

    public boolean snapToGridChanged = false;
    public Boolean oldSnapToGrid;
    public Boolean newSnapToGrid;
}
