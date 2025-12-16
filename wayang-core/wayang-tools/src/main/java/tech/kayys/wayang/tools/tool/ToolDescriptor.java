
/**
 * MCP-compatible tool descriptor
 */
@Immutable
public final class ToolDescriptor {
    private final String id;
    private final String name;
    private final String description;
    private final JsonSchema inputSchema;
    private final JsonSchema outputSchema;
    private final Set<String> capabilities;
    private final ToolMetadata metadata;
    
    // Implementation...
}