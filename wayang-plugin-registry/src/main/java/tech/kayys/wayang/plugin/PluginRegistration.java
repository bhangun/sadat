package tech.kayys.wayang.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Plugin registration result
 */
public class PluginRegistration {
    public String pluginId;
    public String pluginName;
    public String version;
    public String family;
    public List<String> registeredNodes = new ArrayList<>();
    public List<String> registeredExecutors = new ArrayList<>();
    public java.time.Instant registeredAt;
}
