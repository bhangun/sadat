
@Entity
@Table(name = "node_states")
public class NodeState {
    @Id
    @GeneratedValue
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "run_id")
    private ExecutionRun executionRun;
    
    private String nodeId;
    
    @Enumerated(EnumType.STRING)
    private NodeStatus status;
    
    private int attempts;
    
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> lastError;
    
    private String checkpointRef;
    
    private Instant startTime;
    private Instant endTime;
    
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payload;
}