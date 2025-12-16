
@Entity
@Table(name = "tools")
public class ToolEntity {
    @Id
    private String id;
    
    private String name;
    private String version;
    private String description;
    
    @Enumerated(EnumType.STRING)
    private ToolType type;
    
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private JsonNode inputSchema;
    
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private JsonNode outputSchema;
    
    @ElementCollection
    @CollectionTable(name = "tool_secrets")
    private List<String> requiredSecrets;
    
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private ToolEndpoint endpoint;
    
    @CreationTimestamp
    private Instant createdAt;
    
    @UpdateTimestamp
    private Instant updatedAt;
}