package tech.kayys.wayang.mcp.plugin.builtin;

import tech.kayys.wayang.mcp.plugin.*;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class DefaultTemplateProcessor implements TemplateProcessor {

    @Inject
    Engine quteEngine;

    private final Map<String, TemplateFunction> customFunctions = new ConcurrentHashMap<>();

    @Override
    public String getTemplateType() {
        return "qute";
    }

    @Override
    public void initialize() throws PluginException {
        // Add built-in template functions
        addCustomFunction("camelCase", args -> {
            if (args.length > 0 && args[0] != null) {
                return toCamelCase(args[0].toString());
            }
            return "";
        });

        addCustomFunction("upperCase", args -> {
            if (args.length > 0 && args[0] != null) {
                return args[0].toString().toUpperCase();
            }
            return "";
        });

        addCustomFunction("lowerCase", args -> {
            if (args.length > 0 && args[0] != null) {
                return args[0].toString().toLowerCase();
            }
            return "";
        });

        addCustomFunction("sanitize", args -> {
            if (args.length > 0 && args[0] != null) {
                return args[0].toString().replaceAll("[^a-zA-Z0-9_]", "_");
            }
            return "";
        });
    }

    @Override
    public boolean supports(String templateType) {
        return "qute".equalsIgnoreCase(templateType) ||
                "default".equalsIgnoreCase(templateType);
    }

    @Override
    public String processTemplate(String templateContent, Map<String, Object> data,
            PluginExecutionContext context) throws PluginException {
        try {
            Template template = quteEngine.parse(templateContent);

            // Add custom functions to data context
            Map<String, Object> enrichedData = new java.util.HashMap<>(data);
            enrichedData.putAll(customFunctions);

            return template.data(enrichedData).render();

        } catch (Exception e) {
            throw new PluginException("default-template-processor", "process",
                    "Failed to process template", e);
        }
    }

    @Override
    public void addCustomFunction(String name, TemplateFunction function) {
        customFunctions.put(name, function);
    }

    private String toCamelCase(String input) {
        if (input == null || input.isEmpty())
            return input;

        String[] parts = input.split("[_\\-\\s]+");
        StringBuilder result = new StringBuilder(parts[0].toLowerCase());

        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                result.append(parts[i].substring(0, 1).toUpperCase())
                        .append(parts[i].substring(1).toLowerCase());
            }
        }

        return result.toString();
    }
}
