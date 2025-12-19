package tech.kayys.wayang.agent.audit;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;

@ApplicationScoped
public class AuditService {
    
    public void auditAgentCreation(String userId, String agentId, String agentName, String tenantId) {
        Log.infof("AUDIT: User %s created agent %s (%s) in tenant %s at %s", 
                  userId, agentId, agentName, tenantId, LocalDateTime.now());
    }
    
    public void auditAgentExecution(String userId, String agentId, String tenantId, String executionId) {
        Log.infof("AUDIT: User %s executed agent %s in tenant %s with execution %s at %s", 
                  userId, agentId, tenantId, executionId, LocalDateTime.now());
    }
    
    public void auditIntegrationCreation(String userId, String integrationId, String integrationName, String tenantId) {
        Log.infof("AUDIT: User %s created integration %s (%s) in tenant %s at %s", 
                  userId, integrationId, integrationName, tenantId, LocalDateTime.now());
    }
    
    public void auditBusinessRuleCreation(String userId, String ruleId, String ruleName, String tenantId) {
        Log.infof("AUDIT: User %s created business rule %s (%s) in tenant %s at %s", 
                  userId, ruleId, ruleName, tenantId, LocalDateTime.now());
    }
}