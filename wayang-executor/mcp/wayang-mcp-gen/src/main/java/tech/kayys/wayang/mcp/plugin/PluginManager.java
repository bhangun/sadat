package tech.kayys.wayang.mcp.plugin;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class PluginManager {

    @Inject
    Instance<GeneratorPlugin> plugins;

    @Inject
    Instance<TemplateProcessor> templateProcessors;

    @Inject
    Instance<SpecificationProcessor> specProcessors;

    @Inject
    Instance<ValidationPlugin> validators;

    private final Map<String, GeneratorPlugin> pluginRegistry = new ConcurrentHashMap<>();
    private final Map<String, TemplateProcessor> templateRegistry = new ConcurrentHashMap<>();
    private final Map<String, SpecificationProcessor> specRegistry = new ConcurrentHashMap<>();
    private final Map<String, ValidationPlugin> validatorRegistry = new ConcurrentHashMap<>();

    public void initializePlugins() {
        Log.infof("Initializing plugin system...");

        // Register generator plugins
        if (plugins.isUnsatisfied()) {
            Log.warnf("No generator plugins found");
        } else {
            for (GeneratorPlugin plugin : plugins) {
                try {
                    plugin.initialize();
                    pluginRegistry.put(plugin.getId(), plugin);
                    Log.infof("Registered generator plugin: %s v%s", plugin.getName(), plugin.getVersion());
                } catch (Exception e) {
                    Log.error("Failed to initialize plugin: " + plugin.getClass().getSimpleName(), e);
                }
            }
        }

        // Register template processors
        if (templateProcessors.isUnsatisfied()) {
            Log.warnf("No template processors found");
        } else {
            for (TemplateProcessor processor : templateProcessors) {
                try {
                    processor.initialize();
                    templateRegistry.put(processor.getTemplateType(), processor);
                    Log.infof("Registered template processor: %s", processor.getTemplateType());
                } catch (Exception e) {
                    Log.error("Failed to initialize template processor: " + processor.getClass().getSimpleName(), e);
                }
            }
        }

        // Register specification processors
        if (specProcessors.isUnsatisfied()) {
            Log.warnf("No specification processors found");
        } else {
            for (SpecificationProcessor processor : specProcessors) {
                try {
                    processor.initialize();
                    specRegistry.put(processor.getSpecificationType(), processor);
                    Log.infof("Registered spec processor: %s", processor.getSpecificationType());
                } catch (Exception e) {
                    Log.error("Failed to initialize spec processor: " + processor.getClass().getSimpleName(), e);
                }
            }
        }

        // Register validators
        if (validators.isUnsatisfied()) {
            Log.debugf("No validation plugins found");
        } else {
            for (ValidationPlugin validator : validators) {
                try {
                    validator.initialize();
                    validatorRegistry.put(validator.getValidationType(), validator);
                    Log.infof("Registered validator: %s", validator.getValidationType());
                } catch (Exception e) {
                    Log.error("Failed to initialize validator: " + validator.getClass().getSimpleName(), e);
                }
            }
        }

        Log.infof(
                "Plugin system initialized with %d generator plugins, %d template processors, %d spec processors, %d validators",
                pluginRegistry.size(), templateRegistry.size(), specRegistry.size(), validatorRegistry.size());
    }

    public List<GeneratorPlugin> getAvailablePlugins() {
        return new ArrayList<>(pluginRegistry.values());
    }

    public Optional<GeneratorPlugin> getPlugin(String pluginId) {
        return Optional.ofNullable(pluginRegistry.get(pluginId));
    }

    public List<TemplateProcessor> getTemplateProcessors(String templateType) {
        return templateRegistry.values().stream()
                .filter(processor -> processor.supports(templateType))
                .collect(Collectors.toList());
    }

    public Optional<SpecificationProcessor> getSpecificationProcessor(String specType) {
        return Optional.ofNullable(specRegistry.get(specType));
    }

    public List<ValidationPlugin> getValidators(String validationType) {
        return validatorRegistry.values().stream()
                .filter(validator -> validator.supports(validationType))
                .collect(Collectors.toList());
    }

    public PluginExecutionContext createExecutionContext() {
        return new PluginExecutionContext();
    }
}
