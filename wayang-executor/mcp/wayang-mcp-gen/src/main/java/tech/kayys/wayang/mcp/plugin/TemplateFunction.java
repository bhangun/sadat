package tech.kayys.wayang.mcp.plugin;

@FunctionalInterface
public interface TemplateFunction {
    Object apply(Object... args) throws PluginException;
}
