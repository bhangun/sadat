package tech.kayys.wayang.canvas.schema;

import java.util.HashMap;
import java.util.Map;

/**
 * Canvas layout configuration
 */
public class CanvasLayout {
    public LayoutAlgorithm algorithm = LayoutAlgorithm.MANUAL;
    public LayoutDirection direction = LayoutDirection.TOP_TO_BOTTOM;
    public int nodeSpacing = 100;
    public int rankSpacing = 150;
    public boolean autoLayout = false;
    public Map<String, Object> algorithmConfig = new HashMap<>();
}
