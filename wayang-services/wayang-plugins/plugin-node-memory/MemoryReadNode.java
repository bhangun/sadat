
/**
 * Memory Read Node - Retrieve relevant memories
 * Uses semantic search over stored memories
 */
@ApplicationScoped
@NodeType("builtin.memory.read")
public class MemoryReadNode extends AbstractNode {
    
    @Inject
    MemoryClient memoryClient;
    
    @Override
    protected Uni<ExecutionResult> doExecute(NodeContext context) {
        var query = (String) context.getInput("query");
        var topK = config.getInt("topK", 5);
        var types = config.getList("types", String.class);
        
        var request = MemoryQuery.builder()
            .query(query)
            .topK(topK)
            .types(types != null ? types.stream().map(MemoryType::valueOf).collect(Collectors.toList()) : null)
            .tenantId(context.getTenantId())
            .build();
        
        return memoryClient.query(request)
            .map(memories -> ExecutionResult.success(Map.of(
                "memories", memories,
                "count", memories.size()
            )));
    }
}
