
@Entity
@Table(name = "execution_runs")
public class ExecutionRun {
    @Id
    private UUID runId;
    
    private UUID planId;
    private String planVersion;
    private UUID workflowId;
    
    @Enumerated(EnumType.STRING)
    private RunStatus status;
    
    private UUID tenantId;
    private String createdBy;
    
    @CreationTimestamp
    private Instant createdAt;
    
    private Instant startedAt;
    private Instant completedAt;
    
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    private String error;
    
    @OneToMany(mappedBy = "executionRun", cascade = CascadeType.ALL)
    private List<NodeState> nodeStates;
}
