
@Entity
@Table(name = "agents")
public class AgentEntity {
    @Id
    private String agentId;
    
    private String name;
    private String role;
    
    @ElementCollection
    @CollectionTable(name = "agent_capabilities")
    private Set<String> capabilities;
    
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> configuration;
    
    @Enumerated(EnumType.STRING)
    private AgentStatus status;
    
    private String endpointUrl;
    
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private ResourceProfile resourceProfile;
    
    private UUID tenantId;
    
    @CreationTimestamp
    private Instant createdAt;
    
    @UpdateTimestamp
    private Instant updatedAt;
}