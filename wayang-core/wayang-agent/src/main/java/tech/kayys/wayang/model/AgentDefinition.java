package tech.kayys.wayang.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class AgentDefinition {

    private String id;
    private String name;
    private String description;
    private AgentType type;
    private AgentStatus status;
    private LLMConfig llmConfig;
    private MemoryConfig memoryConfig;
    private List<Tool> tools;
    private List<Workflow> workflows;
    private Personality personality;
    private Capabilities capabilities;
    private Safety safety;
    private Analytics analytics;
    private Deployment deployment;
    private List<Integration> integrations;
    private Permissions permissions;
    private Metadata metadata;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AgentType getType() {
        return type;
    }

    public void setType(AgentType type) {
        this.type = type;
    }

    public AgentStatus getStatus() {
        return status;
    }

    public void setStatus(AgentStatus status) {
        this.status = status;
    }

    public LLMConfig getLlmConfig() {
        return llmConfig;
    }

    public void setLlmConfig(LLMConfig llmConfig) {
        this.llmConfig = llmConfig;
    }

    public MemoryConfig getMemoryConfig() {
        return memoryConfig;
    }

    public void setMemoryConfig(MemoryConfig memoryConfig) {
        this.memoryConfig = memoryConfig;
    }

    public List<Tool> getTools() {
        return tools;
    }

    public void setTools(List<Tool> tools) {
        this.tools = tools;
    }

    public List<Workflow> getWorkflows() {
        return workflows;
    }

    public void setWorkflows(List<Workflow> workflows) {
        this.workflows = workflows;
    }

    public Personality getPersonality() {
        return personality;
    }

    public void setPersonality(Personality personality) {
        this.personality = personality;
    }

    public Capabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Capabilities capabilities) {
        this.capabilities = capabilities;
    }

    public Safety getSafety() {
        return safety;
    }

    public void setSafety(Safety safety) {
        this.safety = safety;
    }

    public Analytics getAnalytics() {
        return analytics;
    }

    public void setAnalytics(Analytics analytics) {
        this.analytics = analytics;
    }

    public Deployment getDeployment() {
        return deployment;
    }

    public void setDeployment(Deployment deployment) {
        this.deployment = deployment;
    }

    public List<Integration> getIntegrations() {
        return integrations;
    }

    public void setIntegrations(List<Integration> integrations) {
        this.integrations = integrations;
    }

    public Permissions getPermissions() {
        return permissions;
    }

    public void setPermissions(Permissions permissions) {
        this.permissions = permissions;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public enum AgentType {
        @JsonProperty("conversational")
        CONVERSATIONAL,
        @JsonProperty("task")
        TASK,
        @JsonProperty("autonomous")
        AUTONOMOUS,
        @JsonProperty("reactive")
        REACTIVE,
        @JsonProperty("multi_agent")
        MULTI_AGENT
    }

    public enum AgentStatus {
        @JsonProperty("draft")
        DRAFT,
        @JsonProperty("active")
        ACTIVE,
        @JsonProperty("paused")
        PAUSED,
        @JsonProperty("archived")
        ARCHIVED
    }

    public static class Metadata {
        private Instant createdAt;
        private Instant updatedAt;
        private String createdBy;
        private String version;
        private List<String> tags;
        private String description;
        private String notes;

        // Getters and Setters
        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }

        public Instant getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    public static class Personality {
        private Tone tone;
        private String style;
        private List<String> guidelines;
        private List<ExampleConversation> exampleConversations;

        public enum Tone {
            @JsonProperty("professional")
            PROFESSIONAL,
            @JsonProperty("casual")
            CASUAL,
            @JsonProperty("friendly")
            FRIENDLY,
            @JsonProperty("formal")
            FORMAL,
            @JsonProperty("humorous")
            HUMOROUS,
            @JsonProperty("custom")
            CUSTOM
        }

        public static class ExampleConversation {
            private String user;
            private String assistant;

            public String getUser() {
                return user;
            }

            public void setUser(String user) {
                this.user = user;
            }

            public String getAssistant() {
                return assistant;
            }

            public void setAssistant(String assistant) {
                this.assistant = assistant;
            }
        }

        // Getters and Setters
        public Tone getTone() {
            return tone;
        }

        public void setTone(Tone tone) {
            this.tone = tone;
        }

        public String getStyle() {
            return style;
        }

        public void setStyle(String style) {
            this.style = style;
        }

        public List<String> getGuidelines() {
            return guidelines;
        }

        public void setGuidelines(List<String> guidelines) {
            this.guidelines = guidelines;
        }

        public List<ExampleConversation> getExampleConversations() {
            return exampleConversations;
        }

        public void setExampleConversations(List<ExampleConversation> exampleConversations) {
            this.exampleConversations = exampleConversations;
        }
    }

    public static class Capabilities {
        private Boolean multimodal;
        private Boolean streaming;
        private Boolean functionCalling;
        private Boolean codeExecution;
        private Boolean webBrowsing;
        private FileHandling fileHandling;

        public static class FileHandling {
            private Boolean upload;
            private List<String> supportedTypes;
            private Integer maxSize;

            public Boolean getUpload() {
                return upload;
            }

            public void setUpload(Boolean upload) {
                this.upload = upload;
            }

            public List<String> getSupportedTypes() {
                return supportedTypes;
            }

            public void setSupportedTypes(List<String> supportedTypes) {
                this.supportedTypes = supportedTypes;
            }

            public Integer getMaxSize() {
                return maxSize;
            }

            public void setMaxSize(Integer maxSize) {
                this.maxSize = maxSize;
            }
        }

        // Getters and Setters
        public Boolean getMultimodal() {
            return multimodal;
        }

        public void setMultimodal(Boolean multimodal) {
            this.multimodal = multimodal;
        }

        public Boolean getStreaming() {
            return streaming;
        }

        public void setStreaming(Boolean streaming) {
            this.streaming = streaming;
        }

        public Boolean getFunctionCalling() {
            return functionCalling;
        }

        public void setFunctionCalling(Boolean functionCalling) {
            this.functionCalling = functionCalling;
        }

        public Boolean getCodeExecution() {
            return codeExecution;
        }

        public void setCodeExecution(Boolean codeExecution) {
            this.codeExecution = codeExecution;
        }

        public Boolean getWebBrowsing() {
            return webBrowsing;
        }

        public void setWebBrowsing(Boolean webBrowsing) {
            this.webBrowsing = webBrowsing;
        }

        public FileHandling getFileHandling() {
            return fileHandling;
        }

        public void setFileHandling(FileHandling fileHandling) {
            this.fileHandling = fileHandling;
        }
    }

    public static class Safety {
        private Boolean contentFiltering;
        private Boolean piiDetection;
        private Double toxicityThreshold;
        private List<String> allowedTopics;
        private List<String> blockedTopics;
        private List<String> moderationRules;

        // Getters and Setters
        public Boolean getContentFiltering() {
            return contentFiltering;
        }

        public void setContentFiltering(Boolean contentFiltering) {
            this.contentFiltering = contentFiltering;
        }

        public Boolean getPiiDetection() {
            return piiDetection;
        }

        public void setPiiDetection(Boolean piiDetection) {
            this.piiDetection = piiDetection;
        }

        public Double getToxicityThreshold() {
            return toxicityThreshold;
        }

        public void setToxicityThreshold(Double toxicityThreshold) {
            this.toxicityThreshold = toxicityThreshold;
        }

        public List<String> getAllowedTopics() {
            return allowedTopics;
        }

        public void setAllowedTopics(List<String> allowedTopics) {
            this.allowedTopics = allowedTopics;
        }

        public List<String> getBlockedTopics() {
            return blockedTopics;
        }

        public void setBlockedTopics(List<String> blockedTopics) {
            this.blockedTopics = blockedTopics;
        }

        public List<String> getModerationRules() {
            return moderationRules;
        }

        public void setModerationRules(List<String> moderationRules) {
            this.moderationRules = moderationRules;
        }
    }

    public static class Analytics {
        private Boolean enabled;
        private List<String> metrics;
        private LoggingConfig logging;

        public static class LoggingConfig {
            private String level;
            private String destination;

            public String getLevel() {
                return level;
            }

            public void setLevel(String level) {
                this.level = level;
            }

            public String getDestination() {
                return destination;
            }

            public void setDestination(String destination) {
                this.destination = destination;
            }
        }

        // Getters and Setters
        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getMetrics() {
            return metrics;
        }

        public void setMetrics(List<String> metrics) {
            this.metrics = metrics;
        }

        public LoggingConfig getLogging() {
            return logging;
        }

        public void setLogging(LoggingConfig logging) {
            this.logging = logging;
        }
    }

    public static class Deployment {
        private String environment;
        private ScalingConfig scalingConfig;
        private List<Endpoint> endpoints;

        public static class ScalingConfig {
            private Integer minInstances;
            private Integer maxInstances;
            private Boolean autoscale;

            public Integer getMinInstances() {
                return minInstances;
            }

            public void setMinInstances(Integer minInstances) {
                this.minInstances = minInstances;
            }

            public Integer getMaxInstances() {
                return maxInstances;
            }

            public void setMaxInstances(Integer maxInstances) {
                this.maxInstances = maxInstances;
            }

            public Boolean getAutoscale() {
                return autoscale;
            }

            public void setAutoscale(Boolean autoscale) {
                this.autoscale = autoscale;
            }
        }

        public static class Endpoint {
            private String type;
            private String url;
            private Map<String, Object> authentication;

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            public Map<String, Object> getAuthentication() {
                return authentication;
            }

            public void setAuthentication(Map<String, Object> authentication) {
                this.authentication = authentication;
            }
        }

        // Getters and Setters
        public String getEnvironment() {
            return environment;
        }

        public void setEnvironment(String environment) {
            this.environment = environment;
        }

        public ScalingConfig getScalingConfig() {
            return scalingConfig;
        }

        public void setScalingConfig(ScalingConfig scalingConfig) {
            this.scalingConfig = scalingConfig;
        }

        public List<Endpoint> getEndpoints() {
            return endpoints;
        }

        public void setEndpoints(List<Endpoint> endpoints) {
            this.endpoints = endpoints;
        }
    }

    public static class Integration {
        private String type;
        private String name;
        private Map<String, Object> config;
        private Boolean enabled;

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, Object> getConfig() {
            return config;
        }

        public void setConfig(Map<String, Object> config) {
            this.config = config;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Permissions {
        private List<Role> roles;
        private List<UserPermission> users;

        public static class Role {
            private String name;
            private List<String> permissions;

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public List<String> getPermissions() {
                return permissions;
            }

            public void setPermissions(List<String> permissions) {
                this.permissions = permissions;
            }
        }

        public static class UserPermission {
            private String userId;
            private String role;

            public String getUserId() {
                return userId;
            }

            public void setUserId(String userId) {
                this.userId = userId;
            }

            public String getRole() {
                return role;
            }

            public void setRole(String role) {
                this.role = role;
            }
        }

        // Getters and Setters
        public List<Role> getRoles() {
            return roles;
        }

        public void setRoles(List<Role> roles) {
            this.roles = roles;
        }

        public List<UserPermission> getUsers() {
            return users;
        }

        public void setUsers(List<UserPermission> users) {
            this.users = users;
        }
    }
}