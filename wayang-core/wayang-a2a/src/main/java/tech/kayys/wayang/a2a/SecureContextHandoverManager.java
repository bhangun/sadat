
// Context Handover Manager
@ApplicationScoped
public class SecureContextHandoverManager {
    @Inject RedactionEngine redactionEngine;
    @Inject PolicyEngine policyEngine;
    
    public A2AContext prepare(A2AMessage message, AgentDescriptor target) {
        // Extract context from message
        Map<String, Object> context = message.getPayload();
        
        // Get handover policy
        HandoverPolicy policy = policyEngine.getHandoverPolicy(
            message.getFromAgent(),
            target.getAgentId()
        );
        
        // Redact based on policy
        Map<String, Object> redacted = redactionEngine.redact(
            context,
            policy.getAllowedFields()
        );
        
        return A2AContext.builder()
            .context(redacted)
            .sourceAgent(message.getFromAgent())
            .targetAgent(target.getAgentId())
            .capabilities(target.getCapabilities())
            .deadline(message.getContextHints().getDeadline())
            .build();
    }
}
