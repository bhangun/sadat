
/**
 * Tool node - executes external tools via MCP
 */
public class ToolNode extends AbstractNode {
    private final ToolGateway toolGateway;
    private final ToolDescriptor toolDescriptor;
    
    public ToolNode(String nodeId, NodeDescriptor descriptor, 
                   ToolGateway toolGateway, ToolDescriptor toolDescriptor) {
        super(nodeId, descriptor);
        this.toolGateway = requireNonNull(toolGateway);
        this.toolDescriptor = requireNonNull(toolDescriptor);
    }
    
    @Override
    protected ExecutionResult doExecute(NodeContext context) throws Exception {
        // Build tool request
        ToolRequest request = buildToolRequest(context);
        
        // Execute tool via gateway
        ToolResponse response = toolGateway.execute(request);
        
        // Convert to execution result
        return convertToExecutionResult(response);
    }
    
    private ToolRequest buildToolRequest(NodeContext context) {
        return ToolRequest.builder()
                .toolId(toolDescriptor.getId())
                .parameters(context.getInputs())
                .build();
    }
}
