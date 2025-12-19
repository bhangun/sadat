package tech.kayys.wayang.schema;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * UIDefinitionDTO - UI layout information
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UIDefinitionDTO {

    private CanvasStateDTO canvas;
    private List<NodeUIDTO> nodes = new ArrayList<>();
    private List<ConnectionUIDTO> connections = new ArrayList<>();

    // Getters and setters...
    public CanvasStateDTO getCanvas() {
        return canvas;
    }

    public void setCanvas(CanvasStateDTO canvas) {
        this.canvas = canvas;
    }

    public List<NodeUIDTO> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeUIDTO> nodes) {
        this.nodes = nodes;
    }

    public List<ConnectionUIDTO> getConnections() {
        return connections;
    }

    public void setConnections(List<ConnectionUIDTO> connections) {
        this.connections = connections;
    }
}
