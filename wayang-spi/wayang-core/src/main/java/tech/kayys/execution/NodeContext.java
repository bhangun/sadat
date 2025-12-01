package tech.kayys.wayang.core.execution;

public interface NodeContext {
    String getRunId();
    String getNodeId();
    String getTenantId();
    String getUserId();
    String getTraceId();
    Map<String, Object> getVariables();
    ExecutionMetadata getMetadata();
    Binding getBinding(String name);
    void setOutput(String name, Object value);
    Object getInput(String name);
    <T> T getInput(String name, Class<T> type);
    ProvenanceContext getProvenance();
    SecurityContext getSecurityContext();
}
