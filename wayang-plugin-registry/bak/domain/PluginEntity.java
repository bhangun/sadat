package tech.kayys.wayang.plugin.runtime.domain;



/**
 * Plugin Entity - Hibernate Reactive Entity
 */
@Entity
@Table(name = "plugins", indexes = {
    @Index(name = "idx_plugin_id_version", columnList = "plugin_id,version", unique = true),
    @Index(name = "idx_plugin_status", columnList = "status"),
    @Index(name = "idx_plugin_tenant", columnList = "tenant_id")
})
public class PluginEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "plugin_id", nullable = false, length = 255)
    private String pluginId;

    @Column(nullable = false, length = 50)
    private String version;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PluginStatus status = PluginStatus.PENDING;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonObject descriptor;

    @Column(name = "checksum", nullable = false, length = 150)
    private String checksum;

    @Column(length = 1000)
    private String signature;

    @Column(name = "published_by", length = 255)
    private String publishedBy;

    @Column(name = "tenant_id", length = 255)
    private String tenantId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private JsonObject metadata;

    // Getters and setters...

    public static PluginEntity fromDescriptor(PluginDescriptor descriptor) {
        PluginEntity entity = new PluginEntity();
        entity.setPluginId(descriptor.getId());
        entity.setVersion(descriptor.getVersion());
        entity.setName(descriptor.getName());
        entity.setDescription(descriptor.getDescription());
        entity.setChecksum(descriptor.getChecksum());
        entity.setSignature(descriptor.getSignature());
        entity.setPublishedBy(descriptor.getPublishedBy());
        entity.setDescriptor(JsonObject.mapFrom(descriptor));
        return entity;
    }

    public PluginDescriptor toDescriptor() {
        return descriptor.mapTo(PluginDescriptor.class);
    }
}