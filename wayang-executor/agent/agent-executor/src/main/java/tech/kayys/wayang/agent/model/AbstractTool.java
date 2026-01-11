package tech.kayys.wayang.agent.model;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;

/**
 * Abstract base class for tools with common functionality
 */
public abstract class AbstractTool implements Tool {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractTool.class);

    protected final String toolName;
    protected final String toolDescription;

    protected AbstractTool(String name, String description) {
        this.toolName = name;
        this.toolDescription = description;
    }

    @Override
    public String name() {
        return toolName;
    }

    @Override
    public String description() {
        return toolDescription;
    }

    @Override
    public Uni<Boolean> validate(Map<String, Object> arguments) {
        // Default validation: check required parameters
        Map<String, Object> schema = parameterSchema();

        if (schema.containsKey("required")) {
            @SuppressWarnings("unchecked")
            List<String> required = (List<String>) schema.get("required");

            for (String param : required) {
                if (!arguments.containsKey(param) || arguments.get(param) == null) {
                    LOG.warn("Missing required parameter: {}", param);
                    return Uni.createFrom().item(false);
                }
            }
        }

        return Uni.createFrom().item(true);
    }

    /**
     * Helper to get parameter value with type checking
     */
    protected <T> T getParam(Map<String, Object> args, String key, Class<T> type) {
        Object value = args.get(key);
        if (value == null) {
            return null;
        }

        if (type.isInstance(value)) {
            return type.cast(value);
        }

        throw new IllegalArgumentException(
                "Parameter " + key + " must be of type " + type.getSimpleName());
    }

    /**
     * Helper to get parameter with default value
     */
    protected <T> T getParamOrDefault(
            Map<String, Object> args,
            String key,
            T defaultValue) {
        Object value = args.get(key);
        if (value == null) {
            return defaultValue;
        }

        @SuppressWarnings("unchecked")
        T result = (T) value;
        return result;
    }
}
