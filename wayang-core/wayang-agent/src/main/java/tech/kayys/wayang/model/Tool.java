package tech.kayys.wayang.agent.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Tool {

    private String id;
    private String name;
    private ToolType type;
    private String description;
    private Boolean enabled;
    private ToolConfig config;
    private List<ToolParameter> parameters;

    public enum ToolType {
        @JsonProperty("function")
        FUNCTION,
        @JsonProperty("api")
        API,
        @JsonProperty("database")
        DATABASE,
        @JsonProperty("mcp")
        MCP,
        @JsonProperty("web_search")
        WEB_SEARCH,
        @JsonProperty("code_execution")
        CODE_EXECUTION,
        @JsonProperty("custom")
        CUSTOM
    }

    public static class ToolConfig {
        private String endpoint;
        private String method;
        private Map<String, String> headers;
        private Authentication authentication;
        private Map<String, Object> inputSchema;
        private Map<String, Object> outputSchema;
        private Integer timeout;
        private RateLimit rateLimit;

        public static class Authentication {
            private String type;
            private Map<String, Object> credentials;

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public Map<String, Object> getCredentials() {
                return credentials;
            }

            public void setCredentials(Map<String, Object> credentials) {
                this.credentials = credentials;
            }
        }

        public static class RateLimit {
            private Integer requests;
            private Integer period;
            private String unit;

            public Integer getRequests() {
                return requests;
            }

            public void setRequests(Integer requests) {
                this.requests = requests;
            }

            public Integer getPeriod() {
                return period;
            }

            public void setPeriod(Integer period) {
                this.period = period;
            }

            public String getUnit() {
                return unit;
            }

            public void setUnit(String unit) {
                this.unit = unit;
            }
        }

        // Getters and Setters
        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }

        public Authentication getAuthentication() {
            return authentication;
        }

        public void setAuthentication(Authentication authentication) {
            this.authentication = authentication;
        }

        public Map<String, Object> getInputSchema() {
            return inputSchema;
        }

        public void setInputSchema(Map<String, Object> inputSchema) {
            this.inputSchema = inputSchema;
        }

        public Map<String, Object> getOutputSchema() {
            return outputSchema;
        }

        public void setOutputSchema(Map<String, Object> outputSchema) {
            this.outputSchema = outputSchema;
        }

        public Integer getTimeout() {
            return timeout;
        }

        public void setTimeout(Integer timeout) {
            this.timeout = timeout;
        }

        public RateLimit getRateLimit() {
            return rateLimit;
        }

        public void setRateLimit(RateLimit rateLimit) {
            this.rateLimit = rateLimit;
        }
    }

    public static class ToolParameter {
        private String name;
        private String type;
        private Boolean required;
        private String description;
        private Object defaultValue;

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Boolean getRequired() {
            return required;
        }

        public void setRequired(Boolean required) {
            this.required = required;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ToolType getType() {
        return type;
    }

    public void setType(ToolType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public ToolConfig getConfig() {
        return config;
    }

    public void setConfig(ToolConfig config) {
        this.config = config;
    }

    public List<ToolParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<ToolParameter> parameters) {
        this.parameters = parameters;
    }
}
