
public interface A2ARouter {
    CompletableFuture<A2AResponse> route(A2AMessage message);
    void registerAgent(AgentDescriptor agent);
    void unregisterAgent(String agentId);
    List<AgentDescriptor> discoverAgents(AgentQuery query);
}