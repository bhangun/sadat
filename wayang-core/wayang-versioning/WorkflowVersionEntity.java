
@Entity
@Table(name = "workflow_versions")
public class WorkflowVersionEntity {
    @Id
    private UUID id;
    
    private UUID workflowId;
    private String version;
    
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private JsonNode snapshot;
    
    private String createdBy;
    
    @CreationTimestamp
    private Instant createdAt;
    
    @ElementCollection
    @CollectionTable(name = "version_tags")
    private List<String> tags;
}