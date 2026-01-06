package tech.kayys.wayang.plugin.node;

import java.util.HashMap;
import java.util.Map;

import tech.kayys.wayang.plugin.SchemaReference;

/**
 * Node manifest entry
 */
public class NodeManifest {
    public String type;
    public String label;
    public String category;
    public String subCategory;
    public String description;
    public String icon;
    public String color;

    // Schema references (can be inline or file paths)
    public SchemaReference configSchema;
    public SchemaReference inputSchema;
    public SchemaReference outputSchema;

    // Executor binding
    public String executorId;

    // UI binding
    public String widgetId;

    // Node-specific config
    public Map<String, Object> config = new HashMap<>();
}