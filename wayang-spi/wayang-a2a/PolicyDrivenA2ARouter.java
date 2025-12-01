
@ApplicationScoped
public class PolicyDrivenA2ARouter implements A2ARouter {
    @Inject A2AMessageBus messageBus;
    @Inject A2APolicyEngine policyEngine;
    @Inject AgentRegistry agentRegistry;
    @Inject ContextHandoverManager contextHandover;
    @Inject A2ANegotiator negotiator;
    
    @Override
    public CompletableFuture<A2AResponse> route(A2AMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            // Validate message
            validateMessage(message);
            
            // Check policy
            PolicyDecision decision = policyEngine.evaluate(message);
            if (!decision.isAllowed()) {
                throw new A2APolicyViolationException(decision);
            }
            
            // Resolve target agent
            AgentDescriptor target = resolveTarget(message);
            
            // Negotiate if needed
            if (message.getType() == A2AMessageType.NEGOTIATION) {
                return negotiator.negotiate(message, target);
            }
            
            // Handover context
            A2AContext context = contextHandover.prepare(message, target);
            
            // Route message
            return messageBus.send(target.getAgentId(), message, context);
        });
    }
    
    private AgentDescriptor resolveTarget(A2AMessage message) {
        if (message.getToAgent().startsWith("agent:")) {
            // Direct agent ID
            String agentId = message.getToAgent().substring(6);
            return agentRegistry.getAgent(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId));
        } else if (message.getToAgent().startsWith("role:")) {
            // Role-based routing
            String role = message.getToAgent().substring(5);
            return agentRegistry.getAgentByRole(role, message.getCapabilities())
                .orElseThrow(() -> new NoAgentForRoleException(role));
        } else {
            // Capability-based routing
            return agentRegistry.getAgentByCapability(
                message.getCapabilities()
            ).orElseThrow(() -> new NoCapableAgentException());
        }
    }
}