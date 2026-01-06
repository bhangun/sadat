
/**
 * ==============================================
 * TOOL & INTEGRATION NODES
 * ==============================================
 */

/**
 * Tool Call Node - Execute external tools via MCP
 * Supports schema validation and retry logic
 */
@ApplicationScoped
@NodeType("builtin.tool")
public class ToolCallNode extends AbstractNode {
    
    @Inject
    ToolGatewayClient toolGateway;
    
    @Override
    protected Uni<ExecutionResult> doExecute(NodeContext context) {
        var toolName = config.getString("tool");
        var parameters = context.getInput("parameters");
        
        var request = ToolRequest.builder()
            .tool(toolName)
            .parameters(parameters)
            .timeout(config.getInt("timeout", 30000))
            .runId(context.getRunId())
            .tenantId(context.getTenantId())
            .build();
        
        return toolGateway.execute(request)
            .map(response -> {
                if (response.isError()) {
                    return ExecutionResult.error(
                        ErrorPayload.builder()
                            .type(ErrorType.ToolError)
                            .message(response.getError())
                            .retryable(response.isRetryable())
                            .build()
                    );
                }
                
                return ExecutionResult.success(Map.of(
                    "result", response.getResult(),
                    "tool", toolName,
                    "duration", response.getDuration()
                ));
            });
    }
}
