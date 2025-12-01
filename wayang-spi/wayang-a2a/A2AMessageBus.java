public interface A2AMessageBus {
    CompletableFuture<A2AResponse> send(String targetAgentId, A2AMessage message, A2AContext context);
    void subscribe(String agentId, Consumer<A2AMessage> handler);
    void unsubscribe(String agentId);
}
