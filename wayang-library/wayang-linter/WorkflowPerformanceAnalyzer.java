
// Performance Analyzer
@ApplicationScoped
public class WorkflowPerformanceAnalyzer {
    @Inject ExecutionHistoryStore executionHistory;
    @Inject ModelRegistry modelRegistry;
    
    public List<LintIssue> analyze(Workflow workflow) {
        List<LintIssue> issues = new ArrayList<>();
        
        // Check for sequential bottlenecks
        issues.addAll(detectSequentialBottlenecks(workflow));
        
        // Check for expensive operations
        issues.addAll(detectExpensiveOperations(workflow));
        
        // Check for missing caching
        issues.addAll(detectMissingCache(workflow));
        
        return issues;
    }
    
    private List<LintIssue> detectSequentialBottlenecks(Workflow workflow) {
        List<LintIssue> issues = new ArrayList<>();
        
        // Find chains of independent nodes that could be parallelized
        List<List<NodeInstance>> independentChains = findIndependentChains(
            workflow.getDefinition()
        );
        
        for (List<NodeInstance> chain : independentChains) {
            if (chain.size() >= 3) {
                issues.add(LintIssue.builder()
                    .severity(Severity.INFO)
                    .category(LintCategory.PERFORMANCE)
                    .message("Sequential execution of " + chain.size() + 
                             " independent nodes detected")
                    .location(chain.get(0).getNodeId())
                    .recommendation("Consider parallelizing these nodes")
                    .build());
            }
        }
        
        return issues;
    }
    
    public List<Suggestion> suggestImprovements(Workflow workflow) {
        List<Suggestion> suggestions = new ArrayList<>();
        
        // Analyze historical execution data
        Optional<ExecutionStats> stats = executionHistory.getStats(workflow.getId());
        
        if (stats.isPresent()) {
            ExecutionStats execStats = stats.get();
            
            // Find slow nodes
            for (NodeStats nodeStats : execStats.getNodeStats()) {
                if (nodeStats.getAvgDuration().toMillis() > 5000) {
                    suggestions.add(Suggestion.builder()
                        .type(SuggestionType.CHANGE_MODEL)
                        .title("Optimize slow node")
                        .description("Node " + nodeStats.getNodeId() + 
                                   " has average execution time of " + 
                                   nodeStats.getAvgDuration().toMillis() + "ms")
                        .impact(Impact.HIGH)
                        .difficulty(Difficulty.MEDIUM)
                        .nodeId(nodeStats.getNodeId())
                        .build());
                }
            }
        }
        
        return suggestions;
    }
}