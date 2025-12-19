package tech.kayys.wayang.agent.util;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@ApplicationScoped
public class LoggingUtil {
    
    public void logAgentExecution(String agentId, String tenantId, Map<String, Object> inputs, Map<String, Object> outputs) {
        Log.infof("Agent execution - Agent: %s, Tenant: %s, Input size: %d, Output size: %d", 
                  agentId, tenantId, inputs != null ? inputs.size() : 0, outputs != null ? outputs.size() : 0);
    }
    
    public void logAgentCreation(String agentName, String agentType, String tenantId) {
        Log.infof("Agent created - Name: %s, Type: %s, Tenant: %s", agentName, agentType, tenantId);
    }
    
    public void logWorkflowExecution(String workflowId, String status, long durationMs) {
        Log.infof("Workflow execution - ID: %s, Status: %s, Duration: %d ms", workflowId, status, durationMs);
    }
    
    public void logIntegrationEvent(String integrationName, String eventType, boolean success) {
        Log.infof("Integration event - Name: %s, Event: %s, Success: %b", integrationName, eventType, success);
    }
    
    public void logBusinessRuleExecution(String ruleId, Object context, Object result) {
        Log.infof("Business rule execution - ID: %s, Result: %s", ruleId, result != null ? result.toString() : "null");
    }
}