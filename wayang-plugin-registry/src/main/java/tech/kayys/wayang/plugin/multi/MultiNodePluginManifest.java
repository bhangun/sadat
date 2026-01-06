package tech.kayys.wayang.plugin.multi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.plugin.SharedResources;
import tech.kayys.wayang.plugin.executor.ExecutorManifest;
import tech.kayys.wayang.plugin.node.NodeManifest;

/**
 * Complete plugin manifest for multiple nodes
 */
public class MultiNodePluginManifest {

    // Plugin metadata
    public String pluginId;
    public String name;
    public String version;
    public String family; // "ai", "database", "http", "vector"
    public String author;
    public String description;

    // Multiple node definitions
    public List<NodeManifest> nodes = new ArrayList<>();

    // Multiple executors (optional - can share one executor)
    public List<ExecutorManifest> executors = new ArrayList<>();

    // Shared resources
    public SharedResources shared = new SharedResources();

    // Configuration
    public Map<String, Object> config = new HashMap<>();
}
