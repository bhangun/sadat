package tech.kayys.wayang.canvas.schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Node grouping/swimlanes
 */
public class CanvasGroup {
    public String id;
    public String label;
    public List<String> nodeIds = new ArrayList<>();
    public Position position;
    public Dimensions dimensions;
    public GroupStyle style = new GroupStyle();
}