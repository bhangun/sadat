package tech.kayys.wayang.plugin;

import java.security.Permission;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tech.kayys.wayang.plugin.node.NodeDescriptor;


public class PluginDescriptor {
    String id;
    String name;
    String version;
    String description;
    String author;
    List<NodeDescriptor> nodeDescriptors;
    List<String> dependencies;
    Set<Permission> requiredPermissions;
    String signature;
    Map<String, Object> metadata;

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

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public List<NodeDescriptor> getNodeDescriptors() {
        return nodeDescriptors;
    }

    public void setNodeDescriptors(List<NodeDescriptor> nodeDescriptors) {
        this.nodeDescriptors = nodeDescriptors;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public Set<Permission> getRequiredPermissions() {
        return requiredPermissions;
    }

    public void setRequiredPermissions(Set<Permission> requiredPermissions) {
        this.requiredPermissions = requiredPermissions;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public static class Builder {
        private final PluginDescriptor pluginDescriptor;

        public Builder() {
            pluginDescriptor = new PluginDescriptor();
        }

        public Builder id(String id) {
            pluginDescriptor.setId(id);
            return this;
        }

        public Builder name(String name) {
            pluginDescriptor.setName(name);
            return this;
        }

        public Builder version(String version) {
            pluginDescriptor.setVersion(version);
            return this;
        }

        public Builder description(String description) {
            pluginDescriptor.setDescription(description);
            return this;
        }

        public Builder author(String author) {
            pluginDescriptor.setAuthor(author);
            return this;
        }

        public Builder nodeDescriptors(List<NodeDescriptor> nodeDescriptors) {
            pluginDescriptor.setNodeDescriptors(nodeDescriptors);
            return this;
        }

        public Builder dependencies(List<String> dependencies) {
            pluginDescriptor.setDependencies(dependencies);
            return this;
        }

        public Builder requiredPermissions(Set<Permission> requiredPermissions) {
            pluginDescriptor.setRequiredPermissions(requiredPermissions);
            return this;
        }

        public Builder signature(String signature) {
            pluginDescriptor.setSignature(signature);
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            pluginDescriptor.setMetadata(metadata);
            return this;
        }

        public PluginDescriptor build() {
            return pluginDescriptor;
        }
    }
}