
/**
 * Tool plugin - provides custom tools
 */
public interface ToolPlugin extends Plugin {
    /**
     * Get provided tools
     */
    Map<String, ToolDescriptor> getTools();
    
    /**
     * Create tool executor
     */
    ToolExecutor createExecutor(String toolId);
}