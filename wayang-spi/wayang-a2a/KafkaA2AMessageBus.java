
@ApplicationScoped
public class KafkaA2AMessageBus implements A2AMessageBus {
    @Inject @Channel("a2a-messages") 
    Emitter<A2AMessage> emitter;
    
    @Inject @Channel("a2a-responses") 
    Multi<A2AResponse> responseStream;
    
    private final Map<String, CompletableFuture<A2AResponse>> pendingRequests = 
        new ConcurrentHashMap<>();
    
    @Override
    public CompletableFuture<A2AResponse> send(
        String targetAgentId, 
        A2AMessage message, 
        A2AContext context
    ) {
        CompletableFuture<A2AResponse> future = new CompletableFuture<>();
        pendingRequests.put(message.getMessageId(), future);
        
        // Emit message
        emitter.send(message);
        
        // Set timeout
        scheduleTimeout(message.getMessageId(), context.getDeadline());
        
        return future;
    }
    
    @Incoming("a2a-responses")
    public void handleResponse(A2AResponse response) {
        CompletableFuture<A2AResponse> future = 
            pendingRequests.remove(response.getRequestMessageId());
        
        if (future != null) {
            future.complete(response);
        }
    }
}
