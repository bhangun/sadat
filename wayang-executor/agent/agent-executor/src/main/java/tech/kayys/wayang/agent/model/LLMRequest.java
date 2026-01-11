package tech.kayys.wayang.agent.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Request to LLM provider
 */
public record LLMRequest(
        String provider,
        String model,
        List<Message> messages,
        Double temperature,
        Integer maxTokens,
        List<ToolDefinition> tools,
        boolean streaming,
        Map<String, Object> additionalParams) {

    public LLMRequest {
        messages = List.copyOf(messages);
        tools = tools != null ? List.copyOf(tools) : List.of();
        additionalParams = additionalParams != null ? Map.copyOf(additionalParams) : Map.of();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String provider;
        private String model;
        private List<Message> messages = List.of();
        private Double temperature = 0.7;
        private Integer maxTokens = 2000;
        private List<ToolDefinition> tools = List.of();
        private boolean streaming = false;
        private Map<String, Object> additionalParams = Map.of();

        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder messages(List<Message> messages) {
            this.messages = new ArrayList<>(messages);
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder tools(List<ToolDefinition> tools) {
            this.tools = new ArrayList<>(tools);
            return this;
        }

        public Builder streaming(boolean streaming) {
            this.streaming = streaming;
            return this;
        }

        public Builder additionalParams(Map<String, Object> additionalParams) {
            this.additionalParams = new HashMap<>(additionalParams);
            return this;
        }

        public Builder toolChoice(String toolChoice) {
            if (this.additionalParams.isEmpty()) {
                this.additionalParams = new HashMap<>();
            }
            this.additionalParams.put("tool_choice", toolChoice);
            return this;
        }

        public LLMRequest build() {
            return new LLMRequest(
                    provider, model, messages, temperature, maxTokens,
                    tools, streaming, additionalParams);
        }
    }
}
