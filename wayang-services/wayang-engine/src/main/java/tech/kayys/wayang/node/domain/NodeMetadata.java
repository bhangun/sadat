package tech.kayys.wayang.node.domain;

import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import tech.kayys.wayang.schema.node.PluginDescriptor;
import tech.kayys.wayang.service.JsonbConverter;

/**
 * Node metadata entity.
 */
@Entity
@Table(name = "node_registry")
@lombok.Data
@lombok.NoArgsConstructor
public class NodeMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nodeType;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    private Map<String, Object> descriptor;

    private String pluginId;

    @ElementCollection
    private List<String> capabilities;

    private java.time.Instant updatedAt;

    public static NodeMetadata fromPlugin(PluginDescriptor plugin) {
        NodeMetadata metadata = new NodeMetadata();
        metadata.setNodeType(plugin.getId());
        metadata.setName(plugin.getName());
        metadata.setPluginId(plugin.getId());
        metadata.setCapabilities(plugin.getCapabilities());
        metadata.setUpdatedAt(java.time.Instant.now());
        // Store full descriptor as JSON
        return metadata;
    }
}