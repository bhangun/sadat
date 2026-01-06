package tech.kayys.wayang.api.node;

import java.beans.PropertyDescriptor;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import tech.kayys.wayang.api.execution.SandboxLevel;
import tech.kayys.wayang.api.guardrails.GuardrailsConfig;
import tech.kayys.wayang.api.plugin.PortDescriptor;
import tech.kayys.wayang.api.resource.ResourceProfile;

/**
 * Node descriptor - immutable metadata about a node type
 */
public final class NodeDescriptor {
    private final String id;
    private final String name;
    private final String version;

    private final List<PropertyDescriptor> properties;
    private final Set<String> capabilities;
    private final String implementationArtifact;
    private final SandboxLevel sandboxLevel;
    private final ResourceProfile resourceProfile;

    private final GuardrailsConfig guardrailsConfig;

    private String description;
    private List<PortDescriptor> inputs;
    private List<PortDescriptor> outputs;

    private String category;

    public NodeDescriptor(String id, String name, String version,
            List<PropertyDescriptor> properties, Set<String> capabilities,
            String implementationArtifact, SandboxLevel sandboxLevel,
            ResourceProfile resourceProfile, GuardrailsConfig guardrailsConfig) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.guardrailsConfig = guardrailsConfig;
        this.properties = properties;
        this.capabilities = capabilities;
        this.implementationArtifact = implementationArtifact;
        this.sandboxLevel = sandboxLevel;
        this.resourceProfile = resourceProfile;
    }

    public String getId() {
        return id;
    }

    // Compatibility shim: record-style accessor
    public String id() {
        return getId();
    }

    public String getName() {
        return name;
    }

    // Compatibility shim
    public String name() {
        return getName();
    }

    public String getVersion() {
        return version;
    }

    // Compatibility shim
    public String version() {
        return getVersion();
    }

    public List<PropertyDescriptor> getProperties() {
        return properties;
    }

    public Set<String> getCapabilities() {
        return capabilities;
    }

    // Compatibility shim
    public java.util.Set<String> capabilities() {
        return getCapabilities();
    }

    public String getImplementationArtifact() {
        return implementationArtifact;
    }

    // Compatibility shim for older API
    public String implementation() {
        return getImplementationArtifact();
    }

    public SandboxLevel getSandboxLevel() {
        return sandboxLevel;
    }

    // Compatibility shim
    public SandboxLevel sandboxLevel() {
        return getSandboxLevel();
    }

    public ResourceProfile getResourceProfile() {
        return resourceProfile;
    }

    public GuardrailsConfig getGuardrailsConfig() {
        return guardrailsConfig;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<PortDescriptor> getInputs() {
        return inputs;
    }

    // Compatibility shim
    public java.util.List<PortDescriptor> inputs() {
        return getInputs();
    }

    public void setInputs(List<PortDescriptor> inputs) {
        this.inputs = inputs;
    }

    public List<PortDescriptor> getOutputs() {
        return outputs;
    }

    // Compatibility shim
    public java.util.List<PortDescriptor> outputs() {
        return getOutputs();
    }

    public void setOutputs(List<PortDescriptor> outputs) {
        this.outputs = outputs;
    }

    // Compatibility shim for required secrets (may be absent)
    public java.util.List<String> requiredSecrets() {
        return java.util.Collections.emptyList();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name;
        private String version;
        private List<PropertyDescriptor> properties;
        private Set<String> capabilities;
        private String implementationArtifact;
        private SandboxLevel sandboxLevel;
        private ResourceProfile resourceProfile;
        private String description;
        private List<PortDescriptor> inputs;
        private List<PortDescriptor> outputs;
        private String category;
        private GuardrailsConfig guardrailsConfig;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder properties(List<PropertyDescriptor> properties) {
            this.properties = properties;
            return this;
        }

        public Builder capabilities(Set<String> capabilities) {
            this.capabilities = capabilities;
            return this;
        }

        public Builder implementationArtifact(String implementationArtifact) {
            this.implementationArtifact = implementationArtifact;
            return this;
        }

        public Builder sandboxLevel(SandboxLevel sandboxLevel) {
            this.sandboxLevel = sandboxLevel;
            return this;
        }

        public Builder resourceProfile(ResourceProfile resourceProfile) {
            this.resourceProfile = resourceProfile;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder inputs(List<PortDescriptor> inputs) {
            this.inputs = inputs;
            return this;
        }

        public Builder outputs(List<PortDescriptor> outputs) {
            this.outputs = outputs;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder guardrailsConfig(GuardrailsConfig guardrailsConfig) {
            this.guardrailsConfig = guardrailsConfig;
            return this;
        }

        public NodeDescriptor build() {
            NodeDescriptor descriptor = new NodeDescriptor(id, name, version, properties, capabilities,
                    implementationArtifact, sandboxLevel, resourceProfile, guardrailsConfig);
            descriptor.setDescription(description);
            descriptor.setInputs(inputs);
            descriptor.setOutputs(outputs);
            descriptor.setCategory(category);
            return descriptor;
        }
    }

    @JsonProperty("type")
    private String type;

    @JsonProperty("author")
    private Author author;

    @JsonProperty("errorHandling")
    private ErrorHandlingConfig errorHandling;

    @JsonProperty("observability")
    private ObservabilityConfig observability;

    @JsonProperty("audit")
    private AuditConfig audit;

}