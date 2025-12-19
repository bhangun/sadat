package tech.kayys.wayang.schema.llm;

import java.util.HashMap;
import java.util.Map;

public class ModelSpec {
    private String provider;
    private String model;
    private String version;
    private Double temperature;
    private Double topP;
    private Integer maxTokens;
    private Integer tokenBudget;
    private Map<String, Object> additionalProperties = new HashMap<>();

    public ModelSpec() {
    }

    public ModelSpec(String provider, String model) {
        this.provider = provider;
        this.model = model;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        if (provider == null || provider.trim().isEmpty()) {
            throw new IllegalArgumentException("Model provider cannot be empty");
        }
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("Model name cannot be empty");
        }
        this.model = model;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        if (temperature != null && (temperature < 0 || temperature > 2)) {
            throw new IllegalArgumentException("Temperature must be between 0 and 2");
        }
        this.temperature = temperature;
    }

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        if (topP != null && (topP < 0 || topP > 1)) {
            throw new IllegalArgumentException("TopP must be between 0 and 1");
        }
        this.topP = topP;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        if (maxTokens != null && maxTokens < 1) {
            throw new IllegalArgumentException("Max tokens must be at least 1");
        }
        this.maxTokens = maxTokens;
    }

    public Integer getTokenBudget() {
        return tokenBudget;
    }

    public void setTokenBudget(Integer tokenBudget) {
        this.tokenBudget = tokenBudget;
    }

    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    public void addAdditionalProperty(String key, Object value) {
        this.additionalProperties.put(key, value);
    }
}
