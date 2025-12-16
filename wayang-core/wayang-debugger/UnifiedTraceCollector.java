
// Trace Collector
@ApplicationScoped
public class UnifiedTraceCollector {
    @Inject EntityManager entityManager;
    
    @Incoming("execution-traces")
    public void collectTrace(TraceMessage message) {
        // Store trace
        TraceEntity entity = TraceEntity.builder()
            .traceId(UUID.randomUUID())
            .runId(message.getRunId())
            .nodeId(message.getNodeId())
            .timestamp(message.getTimestamp())
            .level(message.getLevel())
            .source(message.getSource())
            .content(message.getContent())
            .build();
        
        entityManager.persist(entity);
    }
    
    public List<RawTrace> getTraces(UUID runId) {
        return entityManager.createQuery(
            "SELECT t FROM TraceEntity t WHERE t.runId = :runId ORDER BY t.timestamp",
            TraceEntity.class
        )
        .setParameter("runId", runId)
        .getResultStream()
        .map(this::toRawTrace)
        .collect(Collectors.toList());
    }
    
    public List<ReasoningStep> getReasoningSteps(UUID runId, String nodeId) {
        return entityManager.createQuery(
            "SELECT t FROM TraceEntity t " +
            "WHERE t.runId = :runId AND t.nodeId = :nodeId " +
            "AND t.source = 'REASONING' " +
            "ORDER BY t.timestamp",
            TraceEntity.class
        )
        .setParameter("runId", runId)
        .setParameter("nodeId", nodeId)
        .getResultStream()
        .map(this::toReasoningStep)
        .collect(Collectors.toList());
    }
}
