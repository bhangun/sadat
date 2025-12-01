
// Dead Code Detector
@ApplicationScoped
public class DeadCodeDetector {
    public List<LintIssue> detect(Workflow workflow) {
        List<LintIssue> issues = new ArrayList<>();
        
        Set<String> reachableNodes = findReachableNodes(workflow.getDefinition());
        
        for (NodeInstance node : workflow.getDefinition().getNodes()) {
            if (!reachableNodes.contains(node.getNodeId())) {
                issues.add(LintIssue.builder()
                    .severity(Severity.WARNING)
                    .category(LintCategory.DEAD_CODE)
                    .message("Unreachable node detected")
                    .location(node.getNodeId())
                    .recommendation("Remove this node or fix connections")
                    .build());
            }
        }
        
        return issues;
    }
    
    public OptimizedGraph removeDeadNodes(WorkflowDefinition definition) {
        Set<String> reachableNodes = findReachableNodes(definition);
        
        List<NodeInstance> liveNodes = definition.getNodes().stream()
            .filter(node -> reachableNodes.contains(node.getNodeId()))
            .collect(Collectors.toList());
        
        List<Edge> liveEdges = definition.getEdges().stream()
            .filter(edge -> reachableNodes.contains(edge.getSourceNodeId()) &&
                           reachableNodes.contains(edge.getTargetNodeId()))
            .collect(Collectors.toList());
        
        WorkflowDefinition optimized = definition.toBuilder()
            .nodes(liveNodes)
            .edges(liveEdges)
            .build();
        
        return OptimizedGraph.builder()
            .definition(optimized)
            .modified(!liveNodes.equals(definition.getNodes()))
            .removedNodeCount(definition.getNodes().size() - liveNodes.size())
            .build();
    }
    
    private Set<String> findReachableNodes(WorkflowDefinition definition) {
        Set<String> reachable = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        
        // Find start nodes
        List<String> startNodes = definition.getNodes().stream()
            .filter(node -> node.getNodeType().equals("START"))
            .map(NodeInstance::getNodeId)
            .collect(Collectors.toList());
        
        queue.addAll(startNodes);
        reachable.addAll(startNodes);
        
        // BFS traversal
        Map<String, List<Edge>> adjacency = buildAdjacencyMap(definition);
        
        while (!queue.isEmpty()) {
            String current = queue.poll();
            List<Edge> outgoing = adjacency.getOrDefault(current, Collections.emptyList());
            
            for (Edge edge : outgoing) {
                if (!reachable.contains(edge.getTargetNodeId())) {
                    reachable.add(edge.getTargetNodeId());
                    queue.add(edge.getTargetNodeId());
                }
            }
        }
        
        return reachable;
    }
}
