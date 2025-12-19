package tech.kayys.wayang.node.executor;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.HashMap;
import java.util.Map;

/**
 * Transform Service - handles data transformations
 */

@ApplicationScoped
public class TransformService {

    private static final Logger LOG = Logger.getLogger(TransformService.class);
    private static final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

    /**
     * Transform data based on configuration
     */
    public Uni<Map<String, Object>> transform(
            Workflow.Node.NodeConfig.TransformConfig config,
            Map<String, Object> input) {

        if (config == null) {
            return Uni.createFrom().item(input);
        }

        return Uni.createFrom().item(() -> {
            switch (config.getType().toLowerCase()) {
                case "javascript":
                    return executeJavaScriptTransform(config.getScript(), input);
                case "mapping":
                    return executeMappingTransform(config.getMapping(), input);
                case "jsonpath":
                    return executeJsonPathTransform(config.getScript(), input);
                default:
                    LOG.warnf("Unsupported transform type: %s", config.getType());
                    return input;
            }
        });
    }

    /**
     * Execute JavaScript transformation
     */
    private Map<String, Object> executeJavaScriptTransform(String script, Map<String, Object> input) {
        try {
            ScriptEngine engine = scriptEngineManager.getEngineByName("javascript");
            engine.put("input", input);
            engine.put("output", new HashMap<String, Object>());

            engine.eval(script);

            @SuppressWarnings("unchecked")
            Map<String, Object> output = (Map<String, Object>) engine.get("output");
            return output != null ? output : input;

        } catch (Exception e) {
            LOG.errorf(e, "JavaScript transform failed");
            throw new RuntimeException("Transform execution failed", e);
        }
    }

    /**
     * Execute mapping transformation
     */
    private Map<String, Object> executeMappingTransform(Map<String, Object> mapping,
            Map<String, Object> input) {
        Map<String, Object> output = new HashMap<>();

        mapping.forEach((targetKey, sourceKey) -> {
            String source = sourceKey.toString();
            Object value = getNestedValue(input, source);
            setNestedValue(output, targetKey, value);
        });

        return output;
    }

    /**
     * Execute JSONPath transformation
     */
    private Map<String, Object> executeJsonPathTransform(String jsonPath, Map<String, Object> input) {
        // Placeholder for JSONPath implementation
        // Would use a library like jayway jsonpath
        return input;
    }

    /**
     * Get nested value from map
     */
    private Object getNestedValue(Map<String, Object> map, String path) {
        String[] parts = path.split("\\.");
        Object current = map;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }

        return current;
    }

    /**
     * Set nested value in map
     */
    private void setNestedValue(Map<String, Object> map, String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = map;

        for (int i = 0; i < parts.length - 1; i++) {
            current = (Map<String, Object>) current.computeIfAbsent(
                    parts[i], k -> new HashMap<String, Object>());
        }

        current.put(parts[parts.length - 1], value);
    }
}