
/**
 * Agent node - wraps an agent for workflow integration
 */
public class AgentNode extends AbstractNode {
    private final Agent agent;
    private final PromptTemplate promptTemplate;
    
    public AgentNode(String nodeId, NodeDescriptor descriptor, Agent agent) {
        super(nodeId, descriptor);
        this.agent = requireNonNull(agent);
        this.promptTemplate = PromptTemplate.fromDescriptor(descriptor);
    }
    
    @Override
    protected ExecutionResult doExecute(NodeContext context) throws Exception {
        // Build agent context from node context
        AgentContext agentContext = buildAgentContext(context);
        
        // Execute agent
        AgentResult result = agent.execute(agentContext);
        
        // Convert agent result to execution result
        return convertToExecutionResult(result);
    }
    
    private AgentContext buildAgentContext(NodeContext context) {
        return AgentContext.builder()
                .runId(context.getRunId())
                .inputs(context.getInputs())
                .build();
    }
}