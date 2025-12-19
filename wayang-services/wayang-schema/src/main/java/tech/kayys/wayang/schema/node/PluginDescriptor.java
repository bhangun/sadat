package tech.kayys.wayang.schema.node;

import java.util.List;

import tech.kayys.wayang.schema.governance.ResourceProfile;

public class PluginDescriptor {
    private String id;
    private String name;
    private String version;
    private String description;
    private PluginImplementation implementation;
    private List<PortDescriptor> inputs;
    private Outputs outputs;
    private List<PropertyDescriptor> properties;
    private List<String> capabilities;
    private String sandboxLevel;
    private ResourceProfile resourceProfile;

    // Getters and setters
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PluginImplementation getImplementation() {
        return implementation;
    }

    public void setImplementation(PluginImplementation implementation) {
        this.implementation = implementation;
    }

    public List<PortDescriptor> getInputs() {
        return inputs;
    }

    public void setInputs(List<PortDescriptor> inputs) {
        this.inputs = inputs;
    }

    public Outputs getOutputs() {
        return outputs;
    }

    public void setOutputs(Outputs outputs) {
        this.outputs = outputs;
    }

    public List<PropertyDescriptor> getProperties() {
        return properties;
    }

    public void setProperties(List<PropertyDescriptor> properties) {
        this.properties = properties;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities;
    }

    public String getSandboxLevel() {
        return sandboxLevel;
    }

    public void setSandboxLevel(String sandboxLevel) {
        this.sandboxLevel = sandboxLevel;
    }

    public ResourceProfile getResourceProfile() {
        return resourceProfile;
    }

    public void setResourceProfile(ResourceProfile resourceProfile) {
        this.resourceProfile = resourceProfile;
    }
}
