
// Tool Registry with MCP Support
@ApplicationScoped
public class MCPToolRegistry {
    @Inject EntityManager entityManager;
    @Inject MCPDiscoveryClient mcpDiscoveryClient;
    
    @Transactional
    public void registerTool(ToolDescriptor descriptor) {
        ToolEntity entity = toEntity(descriptor);
        entityManager.persist(entity);
    }
    
    public Optional<ToolDescriptor> getTool(String toolId) {
        return Optional.ofNullable(
            entityManager.find(ToolEntity.class, toolId)
        ).map(this::toDescriptor);
    }
    
    public List<ToolDescriptor> discoverMCPTools(String mcpServerUrl) {
        // Discover tools from MCP server
        return mcpDiscoveryClient.discoverTools(mcpServerUrl);
    }
}