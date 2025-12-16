package tech.kayys.wayang.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * UIDiff - Changes in UI definition
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UIDiff {

    public boolean changed = false;
    public boolean canvasChanged = false;

    public List<NodePositionChange> nodePositionChanges = new ArrayList<>();
    public List<NodeSizeChange> nodeSizeChanges = new ArrayList<>();
    public List<NodeStyleChange> nodeStyleChanges = new ArrayList<>();
    public List<ConnectionStyleChange> connectionStyleChanges = new ArrayList<>();

    // Canvas changes
    public CanvasDiff canvasDiff;

    /**
     * Count total UI changes
     */
    public int totalChanges() {
        return nodePositionChanges.size() + nodeSizeChanges.size() +
                nodeStyleChanges.size() + connectionStyleChanges.size() +
                (canvasChanged ? 1 : 0);
    }
}
