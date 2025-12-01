public class DAGExecutor {
    private final ExecutionGraph graph;
    private final ExecutionRun run;
    private final RuntimeHub runtimeHub;
    private final Scheduler scheduler;
    private final Checkpointer checkpointer;
    private final EventBus eventBus;
    private final Map<String, NodeState> nodeStates;
    
    public CompletableFuture<ExecutionResult> execute() {
        CompletableFuture<ExecutionResult> future = new CompletableFuture<>();
        
        // Start with root nodes
        List<String> readyNodes = graph.getRootNodes();
        scheduleNodes(readyNodes);
        
        // Monitor completion
        monitorExecution(future);
        
        return future;
    }
    
    private void scheduleNodes(List<String> nodeIds) {
        for (String nodeId : nodeIds) {
            NodeInstance node = graph.getNode(nodeId);
            NodeState state = nodeStates.get(nodeId);
            
            if (state.getStatus() == NodeStatus.PENDING) {
                scheduleNode(node);
            }
        }
    }
    
    private void scheduleNode(NodeInstance node) {
        ExecuteNodeTask task = buildNodeTask(node);
        
        scheduler.schedule(task)
            .thenAccept(result -> handleNodeComplete(node.getNodeId(), result))
            .exceptionally(error -> {
                handleNodeError(node.getNodeId(), error);
                return null;
            });
    }
    
    private void handleNodeComplete(String nodeId, ExecutionResult result) {
        NodeState state = nodeStates.get(nodeId);
        state.setStatus(NodeStatus.SUCCESS);
        state.setResult(result);
        
        // Update dependent nodes
        List<String> dependentNodes = graph.getDependentNodes(nodeId);
        List<String> readyNodes = dependentNodes.stream()
            .filter(this::areAllDependenciesComplete)
            .collect(Collectors.toList());
        
        scheduleNodes(readyNodes);
    }
}
