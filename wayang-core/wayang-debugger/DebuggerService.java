
public interface DebuggerService {
    DebugSession startSession(UUID runId);
    void stopSession(String sessionId);
    DebugSession getSession(String sessionId);
    List<TraceEntry> getTrace(String sessionId);
    NodeExecutionDetail getNodeDetail(String sessionId, String nodeId);
    ReasoningTrace getReasoningTrace(String sessionId, String nodeId);
}
