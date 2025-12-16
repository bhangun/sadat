package tech.kayys.wayang.plugin.node;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Node Factory Registration - Enhanced factory with metadata
 */
public class NodeFactoryRegistration {
    
    
    private NodeFactory factory;
    
    
    private String nodeTypeId;
    
    private String displayName;
    private String description;
    private String icon;
    private String category;
    
    
    private int order = 0;
    
    
    private boolean experimental = false;
    
    
    private boolean deprecated = false;
    
    private String deprecationMessage;
    private String replacementNodeType;
    
    
    private Set<String> tags = new HashSet<>();
    
    
    private Map<String, Object> metadata = new HashMap<>();


    public NodeFactory getFactory() {
        return this.factory;
    }

    public void setFactory(NodeFactory factory) {
        this.factory = factory;
    }

    public String getNodeTypeId() {
        return this.nodeTypeId;
    }

    public void setNodeTypeId(String nodeTypeId) {
        this.nodeTypeId = nodeTypeId;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return this.icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getCategory() {
        return this.category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getOrder() {
        return this.order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isExperimental() {
        return this.experimental;
    }

    public void setExperimental(boolean experimental) {
        this.experimental = experimental;
    }

    public boolean isDeprecated() {
        return this.deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public String getDeprecationMessage() {
        return this.deprecationMessage;
    }

    public void setDeprecationMessage(String deprecationMessage) {
        this.deprecationMessage = deprecationMessage;
    }

    public String getReplacementNodeType() {
        return this.replacementNodeType;
    }

    public void setReplacementNodeType(String replacementNodeType) {
        this.replacementNodeType = replacementNodeType;
    }

    public Set<String> getTags() {
        return this.tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = (tags == null) ? new HashSet<>() : new HashSet<>(tags);
    }

    public void addTag(String tag) {
        if (tag != null) {
            this.tags.add(tag);
        }
    }

    public void addTags(Set<String> tags) {
        if (tags != null) {
            this.tags.addAll(tags);
        }
    }

    public Map<String, Object> getMetadata() {
        return this.metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = (metadata == null) ? new HashMap<>() : new HashMap<>(metadata);
    }

    public void putMetadata(String key, Object value) {
        if (key != null) {
            this.metadata.put(key, value);
        }
    }

    public void removeMetadata(String key) {
        if (key != null) {
            this.metadata.remove(key);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final NodeFactoryRegistration instance = new NodeFactoryRegistration();

        public Builder factory(NodeFactory factory) {
            instance.factory = factory;
            return this;
        }

        public Builder nodeTypeId(String nodeTypeId) {
            instance.nodeTypeId = nodeTypeId;
            return this;
        }

        public Builder displayName(String displayName) {
            instance.displayName = displayName;
            return this;
        }

        public Builder description(String description) {
            instance.description = description;
            return this;
        }

        public Builder icon(String icon) {
            instance.icon = icon;
            return this;
        }

        public Builder category(String category) {
            instance.category = category;
            return this;
        }

        public Builder order(int order) {
            instance.order = order;
            return this;
        }

        public Builder experimental(boolean experimental) {
            instance.experimental = experimental;
            return this;
        }

        public Builder deprecated(boolean deprecated) {
            instance.deprecated = deprecated;
            return this;
        }

        public Builder deprecationMessage(String message) {
            instance.deprecationMessage = message;
            return this;
        }

        public Builder replacementNodeType(String replacement) {
            instance.replacementNodeType = replacement;
            return this;
        }

        public Builder tags(Set<String> tags) {
            instance.setTags(tags);
            return this;
        }

        public Builder addTag(String tag) {
            instance.addTag(tag);
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            instance.setMetadata(metadata);
            return this;
        }

        public Builder putMetadata(String key, Object value) {
            instance.putMetadata(key, value);
            return this;
        }

        public NodeFactoryRegistration build() {
            // defensive copies to protect internal state
            instance.tags = new HashSet<>(instance.tags);
            instance.metadata = new HashMap<>(instance.metadata);
            return instance;
        }
    }
}
