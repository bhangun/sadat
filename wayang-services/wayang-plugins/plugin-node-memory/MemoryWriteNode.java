
/**
 * ==============================================
 * MEMORY & STATE NODES
 * ==============================================
 */

/**
 * Memory Write Node - Store information in agent memory
 * Supports episodic, semantic, and procedural memory types
 */
@ApplicationScoped
@NodeType("builtin.memory.write")
public class MemoryWriteNode extends AbstractNode {
    
    @Inject
    MemoryClient memoryClient;
    
    @Override
    protected Uni<ExecutionResult> doExecute(NodeContext context) {
        var content = context.getInput("content");
        var type = config.getString("type", "episodic");
        var ttl = config.getString("ttl", "P30D");
        
        var memory = Memory.builder()
            .type(MemoryType.valueOf(type.toUpperCase()))
            .content(content)
            .ttl(Duration.parse(ttl))
            .runId(context.getRunId())
            .tenantId(context.getTenantId())
            .metadata(context.getMetadata().toMap())
            .build();
        
        return memoryClient.write(memory)
            .map(memoryId -> ExecutionResult.success(Map.of(
                "memoryId", memoryId,
                "type", type
            )));
    }
}
