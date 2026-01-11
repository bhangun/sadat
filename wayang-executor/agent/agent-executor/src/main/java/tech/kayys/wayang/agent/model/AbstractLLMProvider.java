package tech.kayys.wayang.agent.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base for LLM providers with common functionality
 */
public abstract class AbstractLLMProvider implements LLMProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractLLMProvider.class);

    protected final String providerName;
    protected final Map<String, Object> config;

    protected AbstractLLMProvider(String name, Map<String, Object> config) {
        this.providerName = name;
        this.config = new HashMap<>(config);
    }

    @Override
    public String name() {
        return providerName;
    }

    @Override
    public Map<String, Object> getConfig() {
        return new HashMap<>(config);
    }

    @Override
    public boolean supportsModel(String model) {
        return supportedModels().contains(model);
    }

    /**
     * Validate request before sending
     */
    protected void validateRequest(LLMRequest request) {
        LOG.debug("Validating request for provider: {}", name());
        if (request.messages() == null || request.messages().isEmpty()) {
            throw new IllegalArgumentException("Messages cannot be empty");
        }

        if (!supportsModel(request.model())) {
            throw new IllegalArgumentException(
                    "Model " + request.model() + " not supported by " + name());
        }

        if (!request.tools().isEmpty() && !supportsFunctionCalling()) {
            throw new IllegalArgumentException(
                    "Provider " + name() + " does not support function calling");
        }
    }

    /**
     * Convert messages to provider-specific format
     */
    protected abstract Object convertMessages(List<Message> messages);

    /**
     * Convert tools to provider-specific format
     */
    protected abstract Object convertTools(List<ToolDefinition> tools);

    /**
     * Parse provider response to standard format
     */
    protected abstract LLMResponse parseResponse(Object response);
}
