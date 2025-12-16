
// Diff Engine
@ApplicationScoped
public class WorkflowDiffEngine {
    public ComparisonResult compare(WorkflowDefinition v1, WorkflowDefinition v2) {
        List<Change> changes = new ArrayList<>();
        
        // Compare nodes
        changes.addAll(compareNodes(v1.getNodes(), v2.getNodes()));
        
        // Compare edges
        changes.addAll(compareEdges(v1.getEdges(), v2.getEdges()));
        
        // Compare variables
        changes.addAll(compareVariables(
            v1.getGlobalVariables(),
            v2.getGlobalVariables()
        ));
        
        return ComparisonResult.builder()
            .changes(changes)
            .totalChanges(changes.size())
            .build();
    }
    
    private List<Change> compareNodes(
        List<NodeInstance> nodes1,
        List<NodeInstance> nodes2
    ) {
        List<Change> changes = new ArrayList<>();
        
        Map<String, NodeInstance> map1 = nodes1.stream()
            .collect(Collectors.toMap(NodeInstance::getNodeId, n -> n));
        Map<String, NodeInstance> map2 = nodes2.stream()
            .collect(Collectors.toMap(NodeInstance::getNodeId, n -> n));
        
        // Find added nodes
        for (NodeInstance node : nodes2) {
            if (!map1.containsKey(node.getNodeId())) {
                changes.add(Change.builder()
                    .type(ChangeType.NODE_ADDED)
                    .nodeId(node.getNodeId())
                    .description("Node added: " + node.getNodeType())
                    .build());
            }
        }
        
        // Find removed nodes
        for (NodeInstance node : nodes1) {
            if (!map2.containsKey(node.getNodeId())) {
                changes.add(Change.builder()
                    .type(ChangeType.NODE_REMOVED)
                    .nodeId(node.getNodeId())
                    .description("Node removed: " + node.getNodeType())
                    .build());
            }
        }
        
        // Find modified nodes
        for (NodeInstance node1 : nodes1) {
            NodeInstance node2 = map2.get(node1.getNodeId());
            if (node2 != null && !node1.equals(node2)) {
                changes.add(Change.builder()
                    .type(ChangeType.NODE_MODIFIED)
                    .nodeId(node1.getNodeId())
                    .description("Node modified: " + node1.getNodeType())
                    .before(node1)
                    .after(node2)
                    .build());
            }
        }
        
        return changes;
    }
}