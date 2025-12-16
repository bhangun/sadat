package tech.kayys.wayang.workflow.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.model.Workflow;
import tech.kayys.wayang.agent.model.Workflow.Node;
import tech.kayys.wayang.node.executor.NodeExecutor;
import tech.kayys.wayang.node.executor.NodeExecutorRegistry;
import tech.kayys.wayang.workflow.model.ExecutionContext;
import tech.kayys.wayang.workflow.service.WorkflowRuntimeEngine.ExecutionResult;

import org.jboss.logging.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class WorkflowRuntimeEngine {

    private static final Logger LOG = Logger.getLogger(WorkflowRuntimeEngine.class);

    @Inject
    NodeExecutorRegistry executorRegistry;

    @Inject
    ExecutionContextManager contextManager;

    /**
     * Execute a workflow with given input
     */
    public Uni<ExecutionResult> executeWorkflow(
            Workflow workflow,
            Map<String, Object> input,
            ExecutionContext context) {

        LOG.infof("Starting workflow execution: %s", workflow.getName());

        return Uni.createFrom().item(() -> {
            // Initialize context
            context.setWorkflow(workflow);
            context.setInput(input);
            context.initializeVariables(workflow.getVariables());

            // Find start node
            Workflow.Node startNode = findStartNode(workflow);
            if (startNode == null) {
                throw new IllegalStateException("Workflow must have a start node");
            }

            return startNode;
        })
                .chain(startNode -> executeNode(startNode, workflow, context))
                .map(result -> new ExecutionResult(
                        result.isSuccess(),
                        result.getOutput(),
                        context.getExecutionTrace(),
                        result.getError()))
                .onFailure().recoverWithItem(error -> {
                    LOG.errorf(error, "Workflow execution failed: %s", workflow.getName());
                    return new ExecutionResult(
                            false,
                            null,
                            context.getExecutionTrace(),
                            error.getMessage());
                });
    }

    /**
     * Execute a single node and continue with next nodes
     */
    private Uni<NodeExecutionResult> executeNode(
            Workflow.Node node,
            Workflow workflow,
            ExecutionContext context) {

        LOG.debugf("Executing node: %s (%s)", node.getName(), node.getType());

        // Check if node already executed (prevent loops)
        if (context.isNodeExecuted(node.getId())) {
            return Uni.createFrom().item(NodeExecutionResult.skip(node.getId()));
        }

        // Mark node as executing
        context.markNodeExecuting(node.getId());

        // Get appropriate executor for node type
        NodeExecutor executor = executorRegistry.getExecutor(node.getType());

        return executor.execute(node, context)
                .chain(result -> {
                    // Mark node as executed
                    context.markNodeExecuted(node.getId(), result);

                    // Handle END node
                    if (node.getType() == Workflow.Node.NodeType.END) {
                        return Uni.createFrom().item(result);
                    }

                    // Find next nodes based on edges
                    return findAndExecuteNextNodes(node, workflow, context, result);
                })
                .onFailure().recoverWithUni(error -> handleNodeError(node, error, workflow, context));
    }

    /**
     * Find and execute next nodes based on edges
     */
    private Uni<NodeExecutionResult> findAndExecuteNextNodes(
            Workflow.Node currentNode,
            Workflow workflow,
            ExecutionContext context,
            NodeExecutionResult currentResult) {

        List<Workflow.Edge> outgoingEdges = workflow.getEdges().stream()
                .filter(edge -> edge.getSource().equals(currentNode.getId()))
                .sorted(Comparator
                        .comparing(edge -> edge.getCondition() != null ? edge.getCondition().getPriority() : 0))
                .collect(Collectors.toList());

        if (outgoingEdges.isEmpty()) {
            LOG.warnf("No outgoing edges from node: %s", currentNode.getId());
            return Uni.createFrom().item(currentResult);
        }

        // Evaluate edges and find matching ones
        List<Workflow.Edge> matchingEdges = outgoingEdges.stream()
                .filter(edge -> evaluateEdgeCondition(edge, context, currentResult))
                .collect(Collectors.toList());

        if (matchingEdges.isEmpty()) {
            LOG.warnf("No matching edges from node: %s", currentNode.getId());
            return Uni.createFrom().item(currentResult);
        }

        // Get target nodes
        List<Workflow.Node> nextNodes = matchingEdges.stream()
                .map(edge -> findNodeById(workflow, edge.getTarget()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Handle parallel execution
        if (nextNodes.size() > 1 && shouldExecuteInParallel(currentNode)) {
            return executeNodesInParallel(nextNodes, workflow, context);
        }

        // Execute next node (first matching)
        if (!nextNodes.isEmpty()) {
            return executeNode(nextNodes.get(0), workflow, context);
        }

        return Uni.createFrom().item(currentResult);
    }

    /**
     * Execute multiple nodes in parallel
     */
    private Uni<NodeExecutionResult> executeNodesInParallel(
            List<Workflow.Node> nodes,
            Workflow workflow,
            ExecutionContext context) {

        LOG.debugf("Executing %d nodes in parallel", nodes.size());

        List<Uni<NodeExecutionResult>> executions = nodes.stream()
                .map(node -> executeNode(node, workflow, context))
                .collect(Collectors.toList());

        return Uni.combine().all().unis(executions)
                .combinedWith(results -> {
                    // Aggregate results
                    List<NodeExecutionResult> resultList = (List<NodeExecutionResult>) results;
                    Map<String, Object> aggregatedOutput = new HashMap<>();

                    resultList.forEach(result -> {
                        if (result.getOutput() != null) {
                            aggregatedOutput.putAll(result.getOutput());
                        }
                    });

                    return new NodeExecutionResult(
                            "parallel-" + UUID.randomUUID(),
                            true,
                            aggregatedOutput,
                            null);
                });
    }

    /**
     * Handle node execution errors
     */
    private Uni<NodeExecutionResult> handleNodeError(
            Workflow.Node node,
            Throwable error,
            Workflow workflow,
            ExecutionContext context) {

        LOG.errorf(error, "Node execution failed: %s", node.getName());

        Workflow.Node.ErrorHandling errorHandling = node.getErrorHandling();

        if (errorHandling == null) {
            return Uni.createFrom().failure(error);
        }

        switch (errorHandling.getStrategy()) {
            case "retry":
                return retryNodeExecution(node, workflow, context, errorHandling);

            case "fallback":
                return executeFallbackNode(node, workflow, context, errorHandling);

            case "skip":
                LOG.infof("Skipping failed node: %s", node.getName());
                return Uni.createFrom().item(NodeExecutionResult.skip(node.getId()));

            default:
                return Uni.createFrom().failure(error);
        }
    }

    /**
     * Retry node execution
     */
    private Uni<NodeExecutionResult> retryNodeExecution(
            Workflow.Node node,
            Workflow workflow,
            ExecutionContext context,
            Workflow.Node.ErrorHandling errorHandling) {

        int maxRetries = errorHandling.getMaxRetries() != null ? errorHandling.getMaxRetries() : 3;
        int retryDelay = errorHandling.getRetryDelay() != null ? errorHandling.getRetryDelay() : 1000;

        NodeExecutor executor = executorRegistry.getExecutor(node.getType());

        return executor.execute(node, context)
                .onFailure().retry()
                .atMost(maxRetries)
                .onFailure().invoke(error -> LOG.errorf(error, "Retry failed for node: %s", node.getName()));
    }

    /**
     * Execute fallback node
     */
    private Uni<NodeExecutionResult> executeFallbackNode(
            Workflow.Node node,
            Workflow workflow,
            ExecutionContext context,
            Workflow.Node.ErrorHandling errorHandling) {

        String fallbackNodeId = errorHandling.getFallbackNode();
        if (fallbackNodeId == null) {
            return Uni.createFrom().failure(
                    new IllegalStateException("Fallback node not specified"));
        }

        Workflow.Node fallbackNode = findNodeById(workflow, fallbackNodeId);
        if (fallbackNode == null) {
            return Uni.createFrom().failure(
                    new IllegalStateException("Fallback node not found: " + fallbackNodeId));
        }

        LOG.infof("Executing fallback node: %s", fallbackNode.getName());
        return executeNode(fallbackNode, workflow, context);
    }

    /**
     * Evaluate edge condition
     */
    private boolean evaluateEdgeCondition(
            Workflow.Edge edge,
            ExecutionContext context,
            NodeExecutionResult result) {

        // Default edge always matches
        if (edge.getType() == Workflow.Edge.EdgeType.DEFAULT) {
            return true;
        }

        // Conditional edge
        if (edge.getType() == Workflow.Edge.EdgeType.CONDITIONAL &&
                edge.getCondition() != null) {

            String expression = edge.getCondition().getExpression();
            return context.evaluateExpression(expression, result.getOutput());
        }

        return true;
    }

    /**
     * Check if nodes should execute in parallel
     */
    private boolean shouldExecuteInParallel(Workflow.Node node) {
        return node.getType() == Workflow.Node.NodeType.PARALLEL ||
                (node.getConfig() != null &&
                        node.getConfig().getParallelConfig() != null);
    }

    /**
     * Find start node in workflow
     */
    private Node findStartNode(Workflow workflow) {
        return workflow.getNodes().stream()
                .filter(node -> node.getType() == Workflow.Node.NodeType.START)
                .findFirst()
                .orElse(null);
    }

    /**
     * Find node by ID
     */
    private Node findNodeById(Workflow workflow, String nodeId) {
        return workflow.getNodes().stream()
                .filter(node -> node.getId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Execution result
     */
    public static class ExecutionResult {
        private final boolean success;
        private final Map<String, Object> output;
        private final List<ExecutionTrace> trace;
        private final String error;

        public ExecutionResult(boolean success, Map<String, Object> output,
                List<ExecutionTrace> trace, String error) {
            this.success = success;
            this.output = output;
            this.trace = trace;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public Map<String, Object> getOutput() {
            return output;
        }

        public List<ExecutionTrace> getTrace() {
            return trace;
        }

        public String getError() {
            return error;
        }
    }

    /**
     * Node execution result
     */
    public static class NodeExecutionResult {
        private final String nodeId;
        private final boolean success;
        private final Map<String, Object> output;
        private final String error;

        public NodeExecutionResult(String nodeId, boolean success,
                Map<String, Object> output, String error) {
            this.nodeId = nodeId;
            this.success = success;
            this.output = output;
            this.error = error;
        }

        public static NodeExecutionResult skip(String nodeId) {
            return new NodeExecutionResult(nodeId, true, Map.of(), null);
        }

        public String getNodeId() {
            return nodeId;
        }

        public boolean isSuccess() {
            return success;
        }

        public Map<String, Object> getOutput() {
            return output;
        }

        public String getError() {
            return error;
        }
    }

    /**
     * Execution trace for debugging
     */
    public static class ExecutionTrace {
        private final String nodeId;
        private final String nodeName;
        private final long timestamp;
        private final Map<String, Object> input;
        private final Map<String, Object> output;
        private final String status;

        public ExecutionTrace(String nodeId, String nodeName, long timestamp,
                Map<String, Object> input, Map<String, Object> output,
                String status) {
            this.nodeId = nodeId;
            this.nodeName = nodeName;
            this.timestamp = timestamp;
            this.input = input;
            this.output = output;
            this.status = status;
        }

        // Getters
        public String getNodeId() {
            return nodeId;
        }

        public String getNodeName() {
            return nodeName;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public Map<String, Object> getInput() {
            return input;
        }

        public Map<String, Object> getOutput() {
            return output;
        }

        public String getStatus() {
            return status;
        }
    }
}
