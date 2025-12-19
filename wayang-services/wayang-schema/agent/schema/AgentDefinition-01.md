package tech.kayys.agent.schema;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record AgentDefinition(
        String id,
        String name,
        Optional<String> description,
        List<AgentCapability> capabilities,
        AgentType type,
        List<ToolSpec> tools,
        Optional<MemoryConfig> memory,
        SecurityContext security,
        List<String> allowedActions,
        List<ExtensionPoint> extensions,
        URI schemaRef,
        Optional<String> checksum) {
    public AgentDefinition {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(capabilities, "capabilities must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(tools, "tools must not be null");
        Objects.requireNonNull(memory, "memory must not be null");
        Objects.requireNonNull(security, "security must not be null");
        Objects.requireNonNull(allowedActions, "allowedActions must not be null");
        Objects.requireNonNull(extensions, "extensions must not be null");
        Objects.requireNonNull(schemaRef, "schemaRef must not be null");
        Objects.requireNonNull(checksum, "checksum must not be null");

        // Defensive copies for mutable collections
        capabilities = List.copyOf(capabilities);
        tools = List.copyOf(tools);
        allowedActions = List.copyOf(allowedActions);
        extensions = List.copyOf(extensions);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name;
        private String description;
        private List<AgentCapability> capabilities = new ArrayList<>();
        private AgentType type = AgentType.AGENT;
        private List<ToolSpec> tools = new ArrayList<>();
        private MemoryConfig memory;
        private SecurityContext security = new SecurityContext("system", List.of("admin"), "full", true);
        private List<String> allowedActions = new ArrayList<>();
        private List<ExtensionPoint> extensions = new ArrayList<>();
        private URI schemaRef = URI.create("https://schemas.kayys.tech/agent/v1");
        private String checksum;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String desc) {
            this.description = desc;
            return this;
        }

        public Builder capabilities(List<AgentCapability> caps) {
            this.capabilities = new ArrayList<>(caps);
            return this;
        }

        public Builder addCapability(AgentCapability cap) {
            this.capabilities.add(cap);
            return this;
        }

        public Builder type(AgentType type) {
            this.type = type;
            return this;
        }

        public Builder tools(List<ToolSpec> tools) {
            this.tools = new ArrayList<>(tools);
            return this;
        }

        public Builder addTool(ToolSpec tool) {
            this.tools.add(tool);
            return this;
        }

        public Builder memory(MemoryConfig memory) {
            this.memory = memory;
            return this;
        }

        public Builder security(SecurityContext sec) {
            this.security = sec;
            return this;
        }

        public Builder allowedActions(List<String> actions) {
            this.allowedActions = new ArrayList<>(actions);
            return this;
        }

        public Builder addAllowedAction(String action) {
            this.allowedActions.add(action);
            return this;
        }

        public Builder extensions(List<ExtensionPoint> exts) {
            this.extensions = new ArrayList<>(exts);
            return this;
        }

        public Builder addExtension(ExtensionPoint ext) {
            this.extensions.add(ext);
            return this;
        }

        public Builder schemaRef(URI uri) {
            this.schemaRef = uri;
            return this;
        }

        public Builder checksum(String checksum) {
            this.checksum = checksum;
            return this;
        }

        public AgentDefinition build() {
            return new AgentDefinition(
                    id,
                    name,
                    Optional.ofNullable(description),
                    capabilities,
                    type,
                    tools,
                    Optional.ofNullable(memory),
                    security,
                    allowedActions,
                    extensions,
                    schemaRef,
                    Optional.ofNullable(checksum));
        }
    }
}
