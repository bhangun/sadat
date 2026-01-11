package tech.kayys.wayang.agent.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Runtime context for agent execution
 * Mutable object that accumulates state during execution
 */
public class AgentContext {
    private final String sessionId;
    private final String runId;
    private final String nodeId;
    private final String tenantId;
    private final AgentConfiguration configuration;
    private final Map<String, Object> taskContext;

    // Mutable execution state
    private List<Message> memory;
    private final List<Message> messages;
    private List<Tool> tools;
    private final Map<String, Object> metadata;

    private AgentContext(Builder builder) {
        this.sessionId = builder.sessionId;
        this.runId = builder.runId;
        this.nodeId = builder.nodeId;
        this.tenantId = builder.tenantId;
        this.configuration = builder.configuration;
        this.taskContext = new HashMap<>(builder.taskContext);
        this.memory = new ArrayList<>();
        this.messages = new CopyOnWriteArrayList<>();
        this.tools = new ArrayList<>();
        this.metadata = new HashMap<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String sessionId() {
        return sessionId;
    }

    public String runId() {
        return runId;
    }

    public String nodeId() {
        return nodeId;
    }

    public String tenantId() {
        return tenantId;
    }

    public AgentConfiguration configuration() {
        return configuration;
    }

    public Map<String, Object> taskContext() {
        return taskContext;
    }

    // Memory management
    public boolean hasMemory() {
        return memory != null && !memory.isEmpty();
    }

    public List<Message> getMemory() {
        return new ArrayList<>(memory);
    }

    public void setMemory(List<Message> memory) {
        this.memory = new ArrayList<>(memory);
    }

    // Message management
    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    public void addMessage(Message message) {
        messages.add(message);
    }

    public void addMessage(LLMResponse response) {
        messages.add(Message.assistant(
                response.content(),
                response.toolCalls()));
    }

    public void addToolResult(ToolResult result) {
        messages.add(Message.tool(result.id(), result.output()));
    }

    // Tool management
    public boolean hasTools() {
        return tools != null && !tools.isEmpty();
    }

    public List<Tool> getTools() {
        return new ArrayList<>(tools);
    }

    public void setTools(List<Tool> tools) {
        this.tools = new ArrayList<>(tools);
    }

    // Metadata
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    public Map<String, Object> getAllMetadata() {
        return new HashMap<>(metadata);
    }

    public static class Builder {
        private String sessionId;
        private String runId;
        private String nodeId;
        private String tenantId;
        private AgentConfiguration configuration;
        private Map<String, Object> taskContext = Map.of();

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder runId(String runId) {
            this.runId = runId;
            return this;
        }

        public Builder nodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder configuration(AgentConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder taskContext(Map<String, Object> taskContext) {
            this.taskContext = taskContext;
            return this;
        }

        public AgentContext build() {
            return new AgentContext(this);
        }
    }
}
