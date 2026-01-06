
/**
 * Evaluator node - assesses quality and correctness
 */
public class EvaluatorNode extends AbstractNode {
    private final EvaluationEngine evaluator;
    private final List<EvaluationCriterion> criteria;
    
    public EvaluatorNode(String nodeId, NodeDescriptor descriptor,
                        EvaluationEngine evaluator) {
        super(nodeId, descriptor);
        this.evaluator = requireNonNull(evaluator);
        this.criteria = loadCriteria(descriptor);
    }
    
    @Override
    protected ExecutionResult doExecute(NodeContext context) throws Exception {
        Object output = context.getInput("output");
        Object expectedOutput = context.getInput("expectedOutput");
        
        // Evaluate against criteria
        EvaluationResult evaluation = evaluator.evaluate(
                output, expectedOutput, criteria);
        
        return ExecutionResult.success(Map.of(
                "score", evaluation.getScore(),
                "passed", evaluation.isPassed(),
                "feedback", evaluation.getFeedback()
        ));
    }
}