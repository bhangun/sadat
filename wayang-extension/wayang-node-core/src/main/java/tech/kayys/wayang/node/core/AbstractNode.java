
/**
 * Abstract base implementation for nodes
 */
public abstract class AbstractNode implements Node {
    protected final String nodeId;
    protected final NodeDescriptor descriptor;
    protected NodeConfig config;
    protected final Logger logger;
    protected final NodeMetrics metrics;
    
    protected AbstractNode(String nodeId, NodeDescriptor descriptor) {
        this.nodeId = requireNonNull(nodeId);
        this.descriptor = requireNonNull(descriptor);
        this.logger = LoggerFactory.getLogger(getClass());
        this.metrics = new NodeMetrics(nodeId);
    }
    
    @Override
    public final ExecutionResult execute(NodeContext context) throws NodeExecutionException {
        logger.info("Executing node: {}", nodeId);
        metrics.recordExecutionStart();
        
        try {
            // Pre-execution validation
            ValidationResult validation = validate(context);
            if (!validation.isValid()) {
                return ExecutionResult.validationFailure(validation);
            }
            
            // Execute node logic
            ExecutionResult result = doExecute(context);
            
            metrics.recordExecutionSuccess(result);
            return result;
            
        } catch (Exception e) {
            logger.error("Node execution failed: {}", nodeId, e);
            metrics.recordExecutionFailure(e);
            throw new NodeExecutionException("Node execution failed: " + nodeId, e);
        }
    }
    
    /**
     * Template method for node-specific execution
     */
    protected abstract ExecutionResult doExecute(NodeContext context) throws Exception;
    
    @Override
    public ValidationResult validate(NodeContext context) {
        ValidationResult.Builder builder = ValidationResult.builder();
        
        // Validate required inputs
        descriptor.getInputs().stream()
                 .filter(InputDescriptor::isRequired)
                 .forEach(input -> {
                     if (!context.hasInput(input.getName())) {
                         builder.error("Missing required input: " + input.getName());
                     }
                 });
        
        return builder.build();
    }
}
