
@Entity
@Table(name = "node_schemas")
public class NodeSchemaEntity {
    @Id
    @GeneratedValue
    private Long id;
    
    private String descriptorId;
    private String version;
    
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private JsonNode schema;
    
    @Enumerated(EnumType.STRING)
    private SchemaStatus status;
    
    @CreationTimestamp
    private Instant createdAt;
    
    @UpdateTimestamp
    private Instant updatedAt;
}