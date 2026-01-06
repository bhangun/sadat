

/**
 * Guardrails node - enforces safety and compliance
 */
public class GuardrailsNode extends AbstractNode {
    private final GuardrailsEngine guardrails;
    private final PolicyEngine policyEngine;
    
    public GuardrailsNode(String nodeId, NodeDescriptor descriptor,
                         GuardrailsEngine guardrails, PolicyEngine policyEngine) {
        super(nodeId, descriptor);
        this.guardrails = requireNonNull(guardrails);
        this.policyEngine = requireNonNull(policyEngine);
    }
    
    @Override
    protected ExecutionResult doExecute(NodeContext context) throws Exception {
        Object content = context.getInput("content");
        
        // Pre-check guardrails
        GuardrailResult preCheck = guardrails.check(content, CheckType.PRE);
        if (!preCheck.isAllowed()) {
            return ExecutionResult.blocked(preCheck.getReason());
        }
        
        // Policy evaluation
        PolicyResult policyResult = policyEngine.evaluate(content, context);
        if (!policyResult.isAllowed()) {
            return ExecutionResult.blocked(policyResult.getReason());
        }
        
        return ExecutionResult.success(Map.of(
                "allowed", true,
                "content", content
        ));
    }
}