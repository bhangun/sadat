
@ApplicationScoped
public class ComprehensiveWorkflowLinter implements WorkflowLinter {
    @Inject RuleEngine ruleEngine;
    @Inject PerformanceAnalyzer performanceAnalyzer;
    @Inject CostEstimator costEstimator;
    @Inject DeadCodeDetector deadCodeDetector;
    @Inject GraphOptimizer graphOptimizer;
    
    @Override
    public LintResult lint(Workflow workflow) {
        List<LintIssue> issues = new ArrayList<>();
        
        // Apply rules
        issues.addAll(ruleEngine.check(workflow));
        
        // Performance analysis
        issues.addAll(performanceAnalyzer.analyze(workflow));
        
        // Dead code detection
        issues.addAll(deadCodeDetector.detect(workflow));
        
        // Type checking
        issues.addAll(checkTypeCompatibility(workflow));
        
        // Security checks
        issues.addAll(checkSecurity(workflow));
        
        return LintResult.builder()
            .issues(issues)
            .totalIssues(issues.size())
            .issuesBySeverity(groupBySeverity(issues))
            .build();
    }
    
    @Override
    public List<Suggestion> suggest(Workflow workflow) {
        List<Suggestion> suggestions = new ArrayList<>();
        
        // Cost optimization suggestions
        suggestions.addAll(costEstimator.suggestOptimizations(workflow));
        
        // Performance suggestions
        suggestions.addAll(performanceAnalyzer.suggestImprovements(workflow));
        
        // Graph structure suggestions
        suggestions.addAll(graphOptimizer.suggestImprovements(workflow));
        
        return suggestions;
    }
    
    @Override
    public OptimizationResult optimize(Workflow workflow) {
        WorkflowDefinition optimized = workflow.getDefinition();
        List<Optimization> applied = new ArrayList<>();
        
        // Remove dead nodes
        OptimizedGraph deadCodeRemoved = deadCodeDetector.removeDeadNodes(optimized);
        if (deadCodeRemoved.isModified()) {
            optimized = deadCodeRemoved.getDefinition();
            applied.add(new Optimization(
                OptimizationType.DEAD_CODE_REMOVAL,
                "Removed " + deadCodeRemoved.getRemovedNodeCount() + " dead nodes"
            ));
        }
        
        // Parallelize independent nodes
        OptimizedGraph parallelized = graphOptimizer.parallelizeIndependentNodes(optimized);
        if (parallelized.isModified()) {
            optimized = parallelized.getDefinition();
            applied.add(new Optimization(
                OptimizationType.PARALLELIZATION,
                "Parallelized " + parallelized.getParallelizedCount() + " node groups"
            ));
        }
        
        // Merge consecutive nodes where possible
        OptimizedGraph merged = graphOptimizer.mergeConsecutiveNodes(optimized);
        if (merged.isModified()) {
            optimized = merged.getDefinition();
            applied.add(new Optimization(
                OptimizationType.NODE_MERGING,
                "Merged " + merged.getMergedCount() + " node pairs"
            ));
        }
        
        // Optimize model selection
        OptimizedGraph modelOptimized = costEstimator.optimizeModelSelection(optimized);
        if (modelOptimized.isModified()) {
            optimized = modelOptimized.getDefinition();
            applied.add(new Optimization(
                OptimizationType.MODEL_OPTIMIZATION,
                "Optimized model selection for cost/performance"
            ));
        }
        
        return OptimizationResult.builder()
            .original(workflow.getDefinition())
            .optimized(optimized)
            .optimizations(applied)
            .estimatedImprovement(calculateImprovement(workflow.getDefinition(), optimized))
            .build();
    }
    
    private List<LintIssue> checkTypeCompatibility(Workflow workflow) {
        List<LintIssue> issues = new ArrayList<>();
        
        Map<String, NodeInstance> nodeMap = workflow.getDefinition().getNodes()
            .stream()
            .collect(Collectors.toMap(NodeInstance::getNodeId, n -> n));
        
        for (Edge edge : workflow.getDefinition().getEdges()) {
            NodeInstance source = nodeMap.get(edge.getSourceNodeId());
            NodeInstance target = nodeMap.get(edge.getTargetNodeId());
            
            // Get output type from source
            DataType sourceOutputType = getOutputType(source, edge.getSourcePort());
            
            // Get input type from target
            DataType targetInputType = getInputType(target, edge.getTargetPort());
            
            // Check compatibility
            if (!areTypesCompatible(sourceOutputType, targetInputType)) {
                issues.add(LintIssue.builder()
                    .severity(Severity.ERROR)
                    .category(LintCategory.TYPE_MISMATCH)
                    .message(String.format(
                        "Type mismatch: %s outputs %s but %s expects %s",
                        source.getNodeId(),
                        sourceOutputType,
                        target.getNodeId(),
                        targetInputType
                    ))
                    .location(edge.getId())
                    .build());
            }
        }
        
        return issues;
    }
    
    private List<LintIssue> checkSecurity(Workflow workflow) {
        List<LintIssue> issues = new ArrayList<>();
        
        for (NodeInstance node : workflow.getDefinition().getNodes()) {
            // Check for hardcoded secrets
            if (containsHardcodedSecrets(node.getConfig())) {
                issues.add(LintIssue.builder()
                    .severity(Severity.CRITICAL)
                    .category(LintCategory.SECURITY)
                    .message("Hardcoded secrets detected in node configuration")
                    .location(node.getNodeId())
                    .recommendation("Use SecretManager or environment variables")
                    .build());
            }
            
            // Check for excessive permissions
            if (hasExcessivePermissions(node)) {
                issues.add(LintIssue.builder()
                    .severity(Severity.WARNING)
                    .category(LintCategory.SECURITY)
                    .message("Node has excessive permissions")
                    .location(node.getNodeId())
                    .recommendation("Apply principle of least privilege")
                    .build());
            }
        }
        
        return issues;
    }
}
