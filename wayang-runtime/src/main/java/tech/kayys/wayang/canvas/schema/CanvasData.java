package tech.kayys.wayang.canvas.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Canvas data structure
 */
public class CanvasData {
    public List<CanvasNode> nodes = new ArrayList<>();
    public List<CanvasEdge> edges = new ArrayList<>();
    public CanvasLayout layout = new CanvasLayout();
    public Map<String, Object> variables = new HashMap<>();
    public List<CanvasGroup> groups = new ArrayList<>();
    public List<CanvasAnnotation> annotations = new ArrayList<>();
}