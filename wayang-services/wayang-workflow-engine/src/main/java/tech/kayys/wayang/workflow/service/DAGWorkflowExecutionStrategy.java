package tech.kayys.wayang.workflow.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.schema.node.EdgeDefinition;
import tech.kayys.wayang.schema.node.NodeDefinition;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.workflow.model.ExecutionContext;
import tech.kayys.wayang.workflow.exception.WorkflowDeadlockException;
import tech.kayys.wayang.workflow.executor.NodeExecutionResult;
import tech.kayys.wayang.workflow.executor.NodeExecutor;

import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DAGWorkflowExecutionStrategy - Implements Directed Acyclic Graph execution
 * strategy
 *
 * This is the default strategy that executes workflows as a directed acyclic
 * graph
 * where nodes are executed based on their dependencies defined by edges.
 */
@ApplicationScoped
public class DAGWorkflowExecutionStrategy implements WorkflowExecutionStrategy {

    private static final Logger LOG = Logger.getLogger(DAGWorkflowExecutionStrategy.class);

    @Inject
    NodeExecutor nodeExecutor;

    @Inject
    StateStore stateStore;

    @Override
    public String getStrategyType() {
        return "DAG";
    }

    @Override
    public boolean canHandle(WorkflowDefinition workflow) {
        // This strategy can handle most workflows with nodes and edges
        return workflow.getNodes() != null && workflow.getEdges() != null;
    }

    @Override
    public Uni<WorkflowRun> execute(WorkflowDefinition workflow, Map<String, Object> inputs, ExecutionContext context) {
        // Find entry nodes (nodes with no incoming edges)
        List<NodeDefinition> entryNodes = findEntryNodes(workflow);

        if (entryNodes.isEmpty()) {
            LOG.warnf("No entry nodes found for workflow: %s", workflow.getId());
            return Uni.createFrom().failure(new IllegalArgumentException("No entry nodes found"));
        }

        LOG.debugf("Starting execution with %d entry nodes", entryNodes.size());

        // Execute the workflow graph
        return executeGraph(entryNodes, context, workflow)
                .onItem().transformToUni(finalContext -> {
                    // Return the updated workflow run from context
                    return Uni.createFrom().item(context.getWorkflowRun());
                });
    }

    /**
     * Execute the workflow graph starting from the given nodes
     */
    private Uni<ExecutionContext> executeGraph(
            List<NodeDefinition> currentLevel,
            ExecutionContext context,
            WorkflowDefinition workflow) {

        if (context.isCancelled()) {
            LOG.infof("Workflow execution cancelled for run: %s", context.getExecutionId());
            return Uni.createFrom().item(context);
        }

        if (currentLevel.isEmpty()) {
            LOG.debug("No more nodes to execute");
            return Uni.createFrom().item(context);
        }

        List<Uni<NodeExecutionResult>> nodeExecutions = currentLevel.stream()
                .filter(node -> !context.isNodeExecuted(node.getId()))
                .filter(node -> areNodeDependenciesMet(node, workflow, context))
                .map(node -> executeSingleNode(node, context, workflow))
                .collect(Collectors.toList());

        if (nodeExecutions.isEmpty()) {
            // Deadlock check logic
            if (hasUnexecutedNodes(workflow, context)) {
                return Uni.createFrom().failure(new WorkflowDeadlockException("Deadlock detected"));
            }
            return Uni.createFrom().item(context);
        }

        return Uni.join().all(nodeExecutions).andCollectFailures()
                .onItem().transformToUni(results -> {
                    for (NodeExecutionResult result : results) {
                        context.markNodeExecuted(result.getNodeId(), result);
                    }

                    // Check if any node is awaiting human interaction
                    if (results.stream().anyMatch(NodeExecutionResult::isAwaitingHuman)) {
                        context.setAwaitingHuman(true);
                        context.getWorkflowRun().suspend("Awaiting human review");
                        return Uni.createFrom().item(context);
                    }

                    List<NodeDefinition> nextLevel = determineNextLevel(workflow, context);
                    if (nextLevel.isEmpty()) {
                        return Uni.createFrom().item(context);
                    }
                    return executeGraph(nextLevel, context, workflow);
                })
                .onFailure().recoverWithUni(th -> {
                    LOG.errorf(th, "Node execution failed");
                    return Uni.createFrom().failure(th);
                });
    }

    /**
     * Execute a single node
     */
    private Uni<NodeExecutionResult> executeSingleNode(
            NodeDefinition nodeDef,
            ExecutionContext context,
            WorkflowDefinition workflow) {

        String nodeId = nodeDef.getId();
        if (context.isNodeExecuted(nodeId)) {
            return Uni.createFrom().item(context.getNodeResult(nodeId));
        }

        NodeContext nodeContext = createNodeContext(nodeDef, context, workflow);

        return nodeExecutor.execute(nodeDef, nodeContext)
                .onItem().invoke(result -> {
                    // Update context with node execution result
                    context.markNodeExecuted(nodeId, result);
                })
                .onFailure().recoverWithUni(th -> {
                    LOG.errorf(th, "Node execution failed: %s", nodeId);
                    return Uni.createFrom().item(
                            NodeExecutionResult.error(nodeId,
                                    tech.kayys.wayang.schema.execution.ErrorPayload.builder()
                                            .message(th.getMessage())
                                            .build()));
                });
    }

    /**
     * Check if all dependencies for a node are met
     */
    private boolean areNodeDependenciesMet(NodeDefinition node, WorkflowDefinition workflow, ExecutionContext context) {
        List<EdgeDefinition> incoming = workflow.getEdges().stream()
                .filter(e -> e.getTo().equals(node.getId()))
                .collect(Collectors.toList());

        if (incoming.isEmpty()) {
            return true;
        }

        for (EdgeDefinition edge : incoming) {
            NodeExecutionResult result = context.getNodeResult(edge.getFrom());
            if (result == null || !result.isSuccess()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determine the next level of nodes to execute
     */
    private List<NodeDefinition> determineNextLevel(WorkflowDefinition workflow, ExecutionContext context) {
        return workflow.getNodes().stream()
                .filter(node -> !context.isNodeExecuted(node.getId()))
                .filter(node -> areNodeDependenciesMet(node, workflow, context))
                .collect(Collectors.toList());
    }

    /**
     * Check if there are still unexecuted nodes
     */
    private boolean hasUnexecutedNodes(WorkflowDefinition workflow, ExecutionContext context) {
        return workflow.getNodes().stream().anyMatch(n -> !context.isNodeExecuted(n.getId()));
    }

    /**
     * Find entry nodes (nodes with no incoming edges)
     */
    private List<NodeDefinition> findEntryNodes(WorkflowDefinition workflow) {
        Set<String> targetNodes = workflow.getEdges().stream()
                .map(EdgeDefinition::getTo)
                .collect(Collectors.toSet());

        return workflow.getNodes().stream()
                .filter(n -> !targetNodes.contains(n.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Create a node context for execution
     */
    private NodeContext createNodeContext(NodeDefinition nodeDef, ExecutionContext context,
            WorkflowDefinition workflow) {
        return NodeContext.builder()
                .nodeId(nodeDef.getId())
                .runId(context.getWorkflowRun() != null ? context.getWorkflowRun().getRunId() : null)
                .workflowId(workflow.getId() != null ? workflow.getId().getValue() : null)
                .workflow(workflow)
                .nodeDefinition(nodeDef)
                .inputs(context.getAllVariables())
                .tenantId(context.getTenantId())
                .executionId(context.getExecutionId())
                .userId(context.getWorkflowRun() != null ? context.getWorkflowRun().getTriggeredBy() : null)
                .executionState(context.getAllVariables())
                .metadata(nodeDef.getProperties() != null ? nodeDef.getProperties().stream()
                        .collect(Collectors.toMap(p -> p.getName(), p -> p.getDefault())) : new HashMap<>())
                .tags(new HashMap<>()) // Initialize with empty tags
                .timestamp(System.currentTimeMillis())
                .build();
    }
}