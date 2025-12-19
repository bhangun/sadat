package tech.kayys.wayang.common.spi;

import java.util.UUID;
import java.util.Map;

public interface NodeContext {
    String getNodeId();
    UUID getRunId();
    String getTenantId();

     /**
     * Get input by port name
     */
    <T> T getInput(String portName, Class<T> type);
    
    /**
     * Get input as raw object
     */
    Object getInput(String portName);
    
    /**
     * Access to guardrails for pre/post checks
     */
    Guardrails getGuardrails();
    
    /**
     * Access to provenance service for audit logging
     */
    ProvenanceContext getProvenance();
    
    /**
     * Access to shared workflow state
     */
    Map<String, Object> getWorkflowState();
    
    /**
     * Store intermediate state (for HITL resume)
     */
    void storeIntermediateState(Map<String, Object> state);
}
