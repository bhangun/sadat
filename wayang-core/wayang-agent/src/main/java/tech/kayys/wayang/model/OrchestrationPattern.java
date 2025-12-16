package tech.kayys.wayang.agent.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Orchestration Pattern Configuration
 */
public class OrchestrationPattern {

    private PatternType type;
    private Map<String, Object> config;

    public enum PatternType {
        @JsonProperty("single_agent")
        SINGLE_AGENT,
        @JsonProperty("sequential")
        SEQUENTIAL,
        @JsonProperty("hierarchical")
        HIERARCHICAL,
        @JsonProperty("reflection")
        REFLECTION,
        @JsonProperty("react")
        REACT,
        @JsonProperty("plan_execute")
        PLAN_EXECUTE,
        @JsonProperty("multi_agent")
        MULTI_AGENT,
        @JsonProperty("router")
        ROUTER,
        @JsonProperty("supervisor")
        SUPERVISOR
    }

    // Getters and Setters
    public PatternType getType() {
        return type;
    }

    public void setType(PatternType type) {
        this.type = type;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }
}