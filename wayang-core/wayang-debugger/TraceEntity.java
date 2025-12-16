
@Entity
@Table(name = "execution_traces")
public class TraceEntity {
    @Id
    private UUID traceId;
    
    private UUID runId;
    private String nodeId;
    
    @Column(name = "timestamp")
    private Instant timestamp;
    
    @Enumerated(EnumType.STRING)
    private TraceLevel level;
    
    private String source;
    
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private JsonNode content;
}
