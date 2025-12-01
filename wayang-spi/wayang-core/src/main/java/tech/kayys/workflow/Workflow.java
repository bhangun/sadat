package tech.kayys.wayang.core.workflow;

@Entity
@Table(name = "workflows")
public class Workflow {
    @Id
    private UUID id;
    
    private String name;
    private String version;
    
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private WorkflowDefinition definition;
    
    @Enumerated(EnumType.STRING)
    private WorkflowStatus status;
    
    private UUID tenantId;
    private String createdBy;
    
    @CreationTimestamp
    private Instant createdAt;
    
    @UpdateTimestamp
    private Instant updatedAt;
    
    // Getters, setters, equals, hashCode
}