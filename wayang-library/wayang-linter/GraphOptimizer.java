
// Graph Optimizer
@ApplicationScoped
public class GraphOptimizer {
    public OptimizedGraph parallelizeIndependentNodes(WorkflowDefinition definition) {
        // Find groups of nodes that can run in parallel
        List<Set<String>> parallelGroups = findParallelizableGroups(definition);
        
        if (parallelGroups.isEmpty()) {
            return OptimizedGraph.builder()
                .definition(definition)
                .modified(false)
                .build();
        }
        
        // Create parallel execution groups
        List<NodeInstance> newNodes = new ArrayList<>(definition.getNodes());
        List<Edge> newEdges = new ArrayList<>(definition.getEdges());
        
        for (Set<String> group : parallelGroups) {
            if (group.size() > 1) {
                // Add parallel coordinator node
                NodeInstance parallelNode = createParallelNode(group);
                newNodes.add(parallelNode);
                
                // Rewire edges
                rewireForParallel(group, parallelNode, newEdges);
            }
        }
        
        WorkflowDefinition optimized = definition.toBuilder()
            .nodes(newNodes)
            .edges(newEdges)
            .build();
        
        return OptimizedGraph.builder()
            .definition(optimized)
            .modified(true)
            .parallelizedCount(parallelGroups.size())
            .build();
    }
    
    public List<Suggestion> suggestImprovements(Workflow workflow) {
        List<Suggestion> suggestions = new ArrayList<>();
        
        // Suggest node merging
        List<NodePair> mergeable = findMergeableNodes(workflow.getDefinition());
        for (NodePair pair : mergeable) {
            suggestions.add(Suggestion.builder()
                .type(SuggestionType.MERGE_NODES)
                .title("Merge consecutive nodes")
                .description("Nodes " + pair.getFirst() + " and " + 
                           pair.getSecond() + " can be merged")
                .impact(Impact.MEDIUM)
                .difficulty(Difficulty.EASY)
                .nodeId(pair.getFirst())
                .build());
        }
        
        return suggestions;
    }
}