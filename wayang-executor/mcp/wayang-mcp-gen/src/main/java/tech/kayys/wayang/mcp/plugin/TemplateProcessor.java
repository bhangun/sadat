package tech.kayys.wayang.mcp.plugin;

import java.util.Map;

public interface TemplateProcessor {

    String getTemplateType();

    void initialize() throws PluginException;

    boolean supports(String templateType);

    String processTemplate(String templateContent, Map<String, Object> data, PluginExecutionContext context)
            throws PluginException;

    void addCustomFunction(String name, TemplateFunction function);
}
