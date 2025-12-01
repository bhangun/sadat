
/**
 * Critic node - provides improvement feedback
 */
public class CriticNode extends AbstractNode {
    private final CriticEngine critic;
    
    public CriticNode(String nodeId, NodeDescriptor descriptor,
                     CriticEngine critic) {
        super(nodeId, descriptor);
        this.critic = requireNonNull(critic);
    }
    
    @Override
    protected ExecutionResult doExecute(NodeContext context) throws Exception {
        Object output = context.getInput("output");
        Object context_data = context.getInput("context");
        
        // Critique the output
        CritiqueResult critique = critic.critique(output, context_data);
        
        return ExecutionResult.success(Map.of(
                "score", critique.getScore(),
                "suggestions", critique.getSuggestions(),
                "action", critique.getRecommendedAction()
        ));
    }
}