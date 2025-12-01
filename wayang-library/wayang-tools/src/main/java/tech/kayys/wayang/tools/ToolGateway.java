
public interface ToolGateway {
    CompletableFuture<ToolResult> execute(ToolRequest request);
    List<ToolDescriptor> listTools(ToolQuery query);
    ToolDescriptor describeTool(String toolId);
    void registerTool(ToolDescriptor descriptor);
    void unregisterTool(String toolId);
}