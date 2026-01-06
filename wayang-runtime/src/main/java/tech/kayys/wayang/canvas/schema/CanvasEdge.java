package tech.kayys.wayang.canvas.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enhanced canvas edge with conditional logic
 */
public class CanvasEdge {
    public String id;
    public String source;
    public String sourcePort;
    public String target;
    public String targetPort;
    public EdgeType type = EdgeType.DEFAULT;
    public String label;
    public String condition;
    public EdgeStyle style = new EdgeStyle();
    public Map<String, Object> data = new HashMap<>();

    // Routing
    public List<Position> waypoints = new ArrayList<>();
    public EdgeRouting routing = EdgeRouting.ORTHOGONAL;

    public Map<String, Object> ui;
}