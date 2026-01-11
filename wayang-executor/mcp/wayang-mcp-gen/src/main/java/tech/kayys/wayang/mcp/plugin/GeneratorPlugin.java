package tech.kayys.wayang.mcp.plugin;

import java.util.Map;

public interface GeneratorPlugin {

    String getId();

    String getName();

    String getVersion();

    String getDescription();

    void initialize() throws PluginException;

    void shutdown() throws PluginException;

    boolean supports(String operation);

    PluginResult execute(PluginExecutionContext context) throws PluginException;

    Map<String, Object> getConfiguration();

    void configure(Map<String, Object> config) throws PluginException;
}
