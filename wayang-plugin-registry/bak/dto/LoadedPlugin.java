
/**
 * Loaded Plugin Wrapper
 */
@Data
@Builder
class LoadedPlugin {
    private String pluginId;
    private String version;
    private PluginDescriptor descriptor;
    private Node instance;
    private IsolationStrategy strategy;
    private Instant loadedAt;
}