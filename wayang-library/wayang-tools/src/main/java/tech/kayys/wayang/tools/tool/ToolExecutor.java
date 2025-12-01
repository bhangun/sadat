
/**
 * Tool executor interface
 */
public interface ToolExecutor {
    /**
     * Execute tool with given parameters
     */
    ToolResponse execute(ToolRequest request) throws ToolExecutionException;
    
    /**
     * Validate tool request
     */
    ValidationResult validate(ToolRequest request);
}