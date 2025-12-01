// Base Node Abstractions
package tech.kayys.wayang.core.node;

public abstract class AbstractNode implements Node {
    protected NodeDescriptor descriptor;
    protected NodeConfig config;
    protected Logger logger;
    protected MetricsCollector metrics;
    
    @Override
    public void onLoad(NodeConfig config) throws NodeException {
        this.config = config;
        this.logger = LoggerFactory.getLogger(getClass());
        this.metrics = MetricsCollector.forNode(descriptor.getId());
        validateConfiguration();
        initializeResources();
    }
    
    protected abstract void validateConfiguration() throws NodeException;
    protected abstract void initializeResources() throws NodeException;
    protected abstract ExecutionResult doExecute(NodeContext context) throws NodeExecutionException;
    
    @Override
    public ExecutionResult execute(NodeContext context) throws NodeExecutionException {
        Span span = startExecutionSpan(context);
        try {
            preExecute(context);
            ExecutionResult result = doExecute(context);
            postExecute(context, result);
            return result;
        } finally {
            span.end();
        }
    }
    
    protected void preExecute(NodeContext context) throws NodeExecutionException {}
    protected void postExecute(NodeContext context, ExecutionResult result) {}
}