package tech.kayys.wayang.canvas.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.node.dto.NodePort;

/**
 * canvas node with AI capabilities
 */
public class CanvasNode {
    public String id;
    public String type;
    public String subType; // For specialized node types
    public String label;
    public Map<String, Object> config;
    public Position position;
    public Dimensions dimensions = new Dimensions(200, 100);
    public NodeStyle style = new NodeStyle();
    public List<NodePort> ports = new ArrayList<>();
    public NodeValidation validation;
    public Map<String, Object> ui;

    // AI-enhanced features
    public List<String> aiSuggestions = new ArrayList<>();
    public Double complexityScore;
    public List<String> optimizationHints = new ArrayList<>();

}
