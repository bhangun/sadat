
// Tool Executor
@ApplicationScoped
public class MultiProtocolToolExecutor {
    @Inject RestToolAdapter restAdapter;
    @Inject GraphQLToolAdapter graphqlAdapter;
    @Inject GrpcToolAdapter grpcAdapter;
    @Inject DatabaseToolAdapter databaseAdapter;
    @Inject LocalFunctionAdapter localAdapter;
    
    public ToolResult execute(ToolRequest request, ToolDescriptor tool) {
        ToolAdapter adapter = selectAdapter(tool.getType());
        
        return adapter.execute(request, tool);
    }
    
    private ToolAdapter selectAdapter(ToolType type) {
        switch (type) {
            case REST_API:
                return restAdapter;
            case GRAPHQL:
                return graphqlAdapter;
            case GRPC:
                return grpcAdapter;
            case DATABASE:
                return databaseAdapter;
            case LOCAL_FUNCTION:
            case MCP_TOOL:
                return localAdapter;
            default:
                throw new UnsupportedToolTypeException(type);
        }
    }
}