package tech.kayys.wayang.mcp.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.List;
import java.util.Map;

@ConfigMapping(prefix = "mcp.generator.plugins")
public interface PluginConfiguration {

    @WithDefault("true")
    boolean enabled();

    @WithDefault("builtin")
    List<String> enabledPlugins();

    @WithDefault("30")
    int executionTimeoutSeconds();

    @WithDefault("true")
    boolean allowCustomPlugins();

    Map<String, Map<String, String>> pluginConfigs();
}
