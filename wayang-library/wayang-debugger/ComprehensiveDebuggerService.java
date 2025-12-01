
@ApplicationScoped
public class ComprehensiveDebuggerService implements DebuggerService {
    @Inject TraceCollector traceCollector;
    @Inject LogNormalizer logNormalizer;
    @Inject PromptMasker promptMasker;
    @Inject ProvenanceService provenanceService;
    
    private final Map<String, DebugSession> activeSessions = new ConcurrentHashMap<>();
    
    @Override
    public DebugSession startSession(UUID runId) {
        String sessionId = UUID.randomUUID().toString();
        
        DebugSession session = DebugSession.builder()
            .sessionId(sessionId)
            .runId(runId)
            .startTime(Instant.now())
            .status(DebugStatus.ACTIVE)
            .build();
        
        activeSessions.put(sessionId, session);
        
        // Subscribe to traces for this run
        traceCollector.subscribe(runId, sessionId);
        
        return session;
    }
    
    @Override
    public List<TraceEntry> getTrace(String sessionId) {
        DebugSession session = activeSessions.get(sessionId);
        if (session == null) {
            throw new SessionNotFoundException(sessionId);
        }
        
        // Get raw traces
        List<RawTrace> rawTraces = traceCollector.getTraces(session.getRunId());
        
        // Normalize and mask
        return rawTraces.stream()
            .map(logNormalizer::normalize)
            .map(promptMasker::maskSensitiveData)
            .collect(Collectors.toList());
    }
    
    @Override
    public NodeExecutionDetail getNodeDetail(String sessionId, String nodeId) {
        DebugSession session = activeSessions.get(sessionId);
        if (session == null) {
            throw new SessionNotFoundException(sessionId);
        }
        
        // Get node execution data
        NodeState nodeState = getNodeState(session.getRunId(), nodeId);
        
        // Get provenance
        ProvenanceRecord provenance = provenanceService.getByNodeExecution(
            session.getRunId(),
            nodeId
        );
        
        // Get logs
        List<LogEntry> logs = traceCollector.getNodeLogs(
            session.getRunId(),
            nodeId
        );
        
        return NodeExecutionDetail.builder()
            .nodeId(nodeId)
            .nodeState(nodeState)
            .provenance(provenance)
            .logs(logs)
            .inputs(nodeState.getPayload().get("inputs"))
            .outputs(nodeState.getPayload().get("outputs"))
            .metrics(extractMetrics(nodeState))
            .build();
    }
    
    @Override
    public ReasoningTrace getReasoningTrace(String sessionId, String nodeId) {
        DebugSession session = activeSessions.get(sessionId);
        if (session == null) {
            throw new SessionNotFoundException(sessionId);
        }
        
        // Get reasoning steps (masked for safety)
        List<ReasoningStep> steps = traceCollector.getReasoningSteps(
            session.getRunId(),
            nodeId
        );
        
        // Mask sensitive content
        List<ReasoningStep> masked = steps.stream()
            .map(promptMasker::maskReasoningStep)
            .collect(Collectors.toList());
        
        return ReasoningTrace.builder()
            .nodeId(nodeId)
            .steps(masked)
            .totalTokens(calculateTotalTokens(steps))
            .build();
    }
}