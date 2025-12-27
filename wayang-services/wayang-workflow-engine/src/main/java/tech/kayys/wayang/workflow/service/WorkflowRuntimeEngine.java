package tech.kayys.wayang.workflow.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.workflow.executor.NodeExecutionResult;
import tech.kayys.wayang.workflow.executor.NodeExecutor;
import tech.kayys.wayang.workflow.executor.NodeExecutorRegistry;
import tech.kayys.wayang.workflow.model.ExecutionContext;
import tech.kayys.wayang.common.spi.ExecutionResult;
import tech.kayys.wayang.common.spi.ExecutionResult.Status;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;
import tech.kayys.wayang.schema.node.NodeDefinition;
import tech.kayys.wayang.schema.node.EdgeDefinition;
import tech.kayys.wayang.schema.execution.ErrorHandlingConfig;
import tech.kayys.wayang.schema.execution.ErrorPayload;

import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
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
            WorkflowDefinition workflow,
            Map<String, Object> input,
            ExecutionContext context) {

        LOG.infof("Starting workflow execution: %s", workflow.getName());

        return Uni.createFrom().item(() -> {
            // Initialize context
            context.setWorkflow(workflow);
            context.setInput(input);
            // Input is already added to variables in setInput()

            // Find start node
            NodeDefinition startNode = findStartNode(workflow);
            if (startNode == null) {
                throw new IllegalStateException("Workflow must have a start node");
            }

            return startNode;
        })
                .chain(startNode -> executeNode(startNode, workflow, context))
                .map(result -> new ExecutionResult(
                        result.isSuccess() ? Status.SUCCESS : Status.ERROR,
                        result.getOutput(),
                        context.getExecutionTrace().toString(),
                        result.getError() != null ? ErrorPayload.builder()
                                .type(ErrorPayload.ErrorType.EXECUTION_ERROR)
                                .message(result.getError().getMessage())
                                .build() : null,
                        null,
                        new HashMap<>()))
                .onFailure().recoverWithItem(error -> {
                    LOG.errorf(error, "Workflow execution failed: %s", workflow.getName());
                    return new ExecutionResult(
                            Status.ERROR,
                            null,
                            context.getExecutionTrace().toString(),
                            ErrorPayload.builder()
                                    .type(ErrorPayload.ErrorType.SYSTEM_ERROR)
                                    .message(error.getMessage())
                                    .build(),
                            null,
                            new HashMap<>());
                });
    }

    /**
     * Execute a single node and continue with next nodes
     */
    private Uni<NodeExecutionResult> executeNode(
            NodeDefinition node,
            WorkflowDefinition workflow,
            ExecutionContext context) {

        LOG.debugf("Executing node: %s (%s)", node.getDisplayName(), node.getType());

        // Check if node already executed (prevent loops)
        if (context.isNodeExecuted(node.getId())) {
            return Uni.createFrom().item(NodeExecutionResult.success(node.getId(), Map.of()));
        }

        // Mark node as executing
        context.markNodeExecuting(node.getId());

        // Get appropriate executor for node type
        NodeExecutor executor = executorRegistry.getExecutor(node.getType());

        NodeContext nodeContext = createNodeContext(node, context, workflow);

        return executor.execute(node, nodeContext)
                .chain(result -> {
                    // Mark node as executed
                    context.markNodeExecuted(node.getId(), result);

                    // Handle END node
                    if ("END".equalsIgnoreCase(node.getType())) {
                        return Uni.createFrom().item(result);
                    }

                    // Find next nodes based on edges
                    return findAndExecuteNextNodes(node, workflow, context, result);
                })
                .onFailure().recoverWithUni(error -> handleNodeError(node, error, workflow, context));
    }

    private NodeContext createNodeContext(NodeDefinition node, ExecutionContext context, WorkflowDefinition workflow) {
        return NodeContext.builder()
                .nodeId(node.getId())
                .runId(context.getExecutionId())
                .workflow(workflow)
                .inputs(context.getAllVariables())
                .build();
    }

    /**
     * Find and execute next nodes based on edges
     */
    private Uni<NodeExecutionResult> findAndExecuteNextNodes(
            NodeDefinition currentNode,
            WorkflowDefinition workflow,
            ExecutionContext context,
            NodeExecutionResult currentResult) {

        List<EdgeDefinition> outgoingEdges = workflow.getEdges().stream()
                .filter(edge -> edge.getFrom().equals(currentNode.getId()))
                .collect(Collectors.toList());

        if (outgoingEdges.isEmpty()) {
            LOG.warnf("No outgoing edges from node: %s", currentNode.getId());
            return Uni.createFrom().item(currentResult);
        }

        // Evaluate edges and find matching ones
        List<EdgeDefinition> matchingEdges = outgoingEdges.stream()
                .filter(edge -> evaluateEdgeCondition(edge, context, currentResult))
                .collect(Collectors.toList());

        if (matchingEdges.isEmpty()) {
            LOG.warnf("No matching edges from node: %s", currentNode.getId());
            return Uni.createFrom().item(currentResult);
        }

        // Get target nodes
        List<NodeDefinition> nextNodes = matchingEdges.stream()
                .map(edge -> findNodeById(workflow, edge.getTo()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Handle parallel execution
        if (nextNodes.size() > 1 && shouldExecuteInParallel(currentNode)) {
            return executeNodesInParallel(nextNodes, workflow, context, currentResult);
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
            List<NodeDefinition> nodes,
            WorkflowDefinition workflow,
            ExecutionContext context,
            NodeExecutionResult currentResult) {

        LOG.debugf("Executing %d nodes in parallel", nodes.size());
        // Execute matching nodes in parallel
        List<Uni<NodeExecutionResult>> executions = nodes.stream()
                .map(node -> executeNode(node, workflow, context))
                .collect(Collectors.toList());
        if (executions.isEmpty()) {
            return Uni.createFrom().item(currentResult);
        }

        return Uni.join().all(executions).andCollectFailures()
                .onItem().transform(results -> {
                    Map<String, Object> aggregatedOutput = new HashMap<>();
                    results.forEach(res -> {
                        if (res.getOutput() != null) {
                            aggregatedOutput.putAll(res.getOutput());
                        }
                    });
                    return NodeExecutionResult.success("parallel-" + UUID.randomUUID(), aggregatedOutput);
                });
    }

    /**
     * Handle node execution errors
     */
    private Uni<NodeExecutionResult> handleNodeError(
            NodeDefinition node,
            Throwable error,
            WorkflowDefinition workflow,
            ExecutionContext context) {

        LOG.errorf(error, "Node execution failed: %s", node.getDisplayName());

        ErrorHandlingConfig errorHandling = node.getErrorHandling();

        if (errorHandling == null) {
            return Uni.createFrom().failure(error);
        }

        if (errorHandling.getRetryPolicy() != null) {
            return retryNodeExecution(node, workflow, context, errorHandling);
        } else if (errorHandling.getFallbackNodeId() != null) {
            return executeFallbackNode(node, workflow, context, errorHandling);
        } else {
            return Uni.createFrom().failure(new RuntimeException(error));
        }
    }

    /**
     * Retry node execution
     */
    private Uni<NodeExecutionResult> retryNodeExecution(
            NodeDefinition node,
            WorkflowDefinition workflow,
            ExecutionContext context,
            ErrorHandlingConfig errorHandling) {

        int maxRetries = errorHandling.getRetryPolicy().getMaxAttempts() != null
                ? errorHandling.getRetryPolicy().getMaxAttempts()
                : 3;
        int retryDelay = errorHandling.getRetryPolicy().getInitialDelayMs() != null
                ? errorHandling.getRetryPolicy().getInitialDelayMs()
                : 1000;

        NodeExecutor executor = executorRegistry.getExecutor(node.getType());

        NodeContext nodeContext = createNodeContext(node, context, workflow);

        return executor.execute(node, nodeContext)
                .onFailure().retry()
                .atMost(maxRetries)
                .onFailure().invoke(error -> LOG.errorf(error, "Retry failed for node: %s", node.getDisplayName()));
    }

    /**
     * Execute fallback node
     */
    private Uni<NodeExecutionResult> executeFallbackNode(
            NodeDefinition node,
            WorkflowDefinition workflow,
            ExecutionContext context,
            ErrorHandlingConfig errorHandling) {

        String fallbackNodeId = errorHandling.getFallbackNodeId();
        if (fallbackNodeId == null) {
            return Uni.createFrom().failure(
                    new IllegalStateException("Fallback node not specified"));
        }

        NodeDefinition fallbackNode = findNodeById(workflow, fallbackNodeId);
        if (fallbackNode == null) {
            return Uni.createFrom().failure(
                    new IllegalStateException("Fallback node not found: " + fallbackNodeId));
        }

        LOG.infof("Executing fallback node: %s", fallbackNode.getDisplayName());
        return executeNode(fallbackNode, workflow, context);
    }

    /**
     * Evaluate edge condition
     */
    private boolean evaluateEdgeCondition(
            EdgeDefinition edge,
            ExecutionContext context,
            NodeExecutionResult result) {

        // Conditional edge
        if (edge.getCondition() != null && !edge.getCondition().isEmpty()) {
            return context.evaluateExpression(edge.getCondition(), result.getOutput());
        }

        return true;
    }

    /**
     * Check if nodes should execute in parallel
     */
    private boolean shouldExecuteInParallel(NodeDefinition node) {
        return "PARALLEL".equalsIgnoreCase(node.getType()) ||
                (node.getControlFlow() != null && Boolean.TRUE.equals(node.getControlFlow().getParallel()));
    }

    /**
     * Find start node in workflow
     */
    private NodeDefinition findStartNode(WorkflowDefinition workflow) {
        return workflow.getNodes().stream()
                .filter(node -> "START".equalsIgnoreCase(node.getType()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Find node by ID
     */
    private NodeDefinition findNodeById(WorkflowDefinition workflow, String nodeId) {
        return workflow.getNodes().stream()
                .filter(node -> node.getId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }
}
