package tech.kayys.wayang.agent.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Complete configuration for an agent instance
 * Immutable configuration object built via builder pattern
 */
public record AgentConfiguration(
        String agentId,
        String tenantId,
        String runId,
        String llmProvider,
        String llmModel,
        Double temperature,
        Integer maxTokens,
        boolean memoryEnabled,
        String memoryType,
        Integer memoryWindowSize,
        List<String> enabledTools,
        boolean allowToolCalls,
        String toolExecutionMode,
        String systemPrompt,
        boolean streaming,
        Integer maxIterations,
        Map<String, Object> additionalConfig) {

    public AgentConfiguration {
        // Defensive copies for mutable fields
        enabledTools = List.copyOf(enabledTools != null ? enabledTools : List.of());
        additionalConfig = Map.copyOf(additionalConfig != null ? additionalConfig : Map.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String agentId;
        private String tenantId;
        private String runId;
        private String llmProvider = "openai";
        private String llmModel = "gpt-4";
        private Double temperature = 0.7;
        private Integer maxTokens = 2000;
        private boolean memoryEnabled = true;
        private String memoryType = "buffer";
        private Integer memoryWindowSize = 10;
        private List<String> enabledTools = new ArrayList<>();
        private boolean allowToolCalls = true;
        private String toolExecutionMode = "auto";
        private String systemPrompt = "You are a helpful AI assistant.";
        private boolean streaming = false;
        private Integer maxIterations = 5;
        private Map<String, Object> additionalConfig = new HashMap<>();

        public Builder agentId(String agentId) {
            this.agentId = agentId;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder runId(String runId) {
            this.runId = runId;
            return this;
        }

        public Builder llmProvider(String llmProvider) {
            this.llmProvider = llmProvider;
            return this;
        }

        public Builder llmModel(String llmModel) {
            this.llmModel = llmModel;
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

        public Builder memoryEnabled(boolean memoryEnabled) {
            this.memoryEnabled = memoryEnabled;
            return this;
        }

        public Builder memoryType(String memoryType) {
            this.memoryType = memoryType;
            return this;
        }

        public Builder memoryWindowSize(Integer memoryWindowSize) {
            this.memoryWindowSize = memoryWindowSize;
            return this;
        }

        public Builder enabledTools(List<String> enabledTools) {
            this.enabledTools = new ArrayList<>(enabledTools != null ? enabledTools : List.of());
            return this;
        }

        public Builder allowToolCalls(boolean allowToolCalls) {
            this.allowToolCalls = allowToolCalls;
            return this;
        }

        public Builder toolExecutionMode(String toolExecutionMode) {
            this.toolExecutionMode = toolExecutionMode;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder streaming(boolean streaming) {
            this.streaming = streaming;
            return this;
        }

        public Builder maxIterations(Integer maxIterations) {
            this.maxIterations = maxIterations;
            return this;
        }

        public Builder additionalConfig(Map<String, Object> additionalConfig) {
            this.additionalConfig = new HashMap<>(additionalConfig);
            return this;
        }

        public Builder config(String key, Object value) {
            this.additionalConfig.put(key, value);
            return this;
        }

        public AgentConfiguration build() {
            return new AgentConfiguration(
                    agentId, tenantId, runId, llmProvider, llmModel,
                    temperature, maxTokens, memoryEnabled, memoryType,
                    memoryWindowSize, enabledTools, allowToolCalls,
                    toolExecutionMode, systemPrompt, streaming, maxIterations, additionalConfig);
        }
    }
}
