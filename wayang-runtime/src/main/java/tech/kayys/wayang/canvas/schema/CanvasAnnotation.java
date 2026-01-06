package tech.kayys.wayang.canvas.schema;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Canvas annotations/comments
 */
public class CanvasAnnotation {
    public String id;
    public String text;
    public Position position;
    public Dimensions dimensions;
    public String createdBy;
    public Instant createdAt;
    public List<String> attachedToNodeIds = new ArrayList<>();
    public AnnotationStyle style = new AnnotationStyle();
}
