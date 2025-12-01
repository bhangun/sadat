

@Entity
@Table(name = "plugins")
public class PluginEntity {
    @Id
    private String pluginId;
    
    private String name;
    private String version;
    
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private PluginDescriptor descriptor;
    
    private String artifactId;
    
    @Enumerated(EnumType.STRING)
    private PluginStatus status;
    
    private UUID approvalRequestId;
    
    @CreationTimestamp
    private Instant createdAt;
    
    @UpdateTimestamp
    private Instant updatedAt;
}