package tech.kayys.wayang.workflow.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.schema.node.NodeDefinition;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.workflow.model.ExecutionContext;
import tech.kayys.wayang.workflow.executor.NodeExecutionResult;
import tech.kayys.wayang.workflow.executor.NodeExecutor;

import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * StateMachineWorkflowExecutionStrategy - Implements state machine execution
 * pattern
 *
 * This strategy executes workflows as finite state machines where nodes
 * represent states
 * and edges represent valid transitions between states. Execution follows state
 * transition
 * rules with support for guards and conditions.
 */
@ApplicationScoped
public class StateMachineWorkflowExecutionStrategy implements WorkflowExecutionStrategy {

    private static final Logger LOG = Logger.getLogger(StateMachineWorkflowExecutionStrategy.class);

    @Inject
    NodeExecutor nodeExecutor;

    @Inject
    StateStore stateStore;

    @Override
    public String getStrategyType() {
        return "STATE_MACHINE";
    }

    @Override
    public boolean canHandle(WorkflowDefinition workflow) {
        // Check if workflow has state machine metadata
        if (workflow.getMetadata() != null) {
            return "STATE_MACHINE".equals(workflow.getMetadata().get("executionStrategy"));
        }
        return false;
    }

    @Override
    public Uni<WorkflowRun> execute(WorkflowDefinition workflow, Map<String, Object> inputs, ExecutionContext context) {
        // Find initial state
        Optional<NodeDefinition> initialState = findInitialState(workflow);

        if (initialState.isEmpty()) {
            LOG.warnf("No initial state found for workflow: %s", workflow.getId());
            return Uni.createFrom().failure(new IllegalArgumentException("No initial state found"));
        }

        LOG.debugf("Starting state machine execution with initial state: %s", initialState.get().getId());

        // Execute the state machine
        return executeStateMachine(initialState.get(), context, workflow)
                .onItem().transformToUni(finalContext -> stateStore.save(finalContext.getWorkflowRun()));
    }

    /**
     * Execute the state machine starting from the given state
     */
    private Uni<ExecutionContext> executeStateMachine(
            NodeDefinition currentState,
            ExecutionContext context,
            WorkflowDefinition workflow) {

        if (context.isCancelled()) {
            LOG.infof("State machine execution cancelled for run: %s", context.getExecutionId());
            return Uni.createFrom().item(context);
        }

        // Check if already executed
        if (context.isNodeExecuted(currentState.getId())) {
            LOG.debugf("State %s already executed", currentState.getId());
            return Uni.createFrom().item(context);
        }

        // Check if this is a final state
        if (isFinalState(currentState, workflow)) {
            LOG.debugf("Reached final state: %s", currentState.getId());
            return executeSingleState(currentState, context, workflow)
                    .onItem().transform(result -> {
                        context.markNodeExecuted(currentState.getId(), result);
                        return context;
                    });
        }

        // Execute current state
        return executeSingleState(currentState, context, workflow)
                .onItem().transformToUni(result -> {
                    context.markNodeExecuted(currentState.getId(), result);

                    // Check if awaiting human interaction
                    if (result.isAwaitingHuman()) {
                        context.setAwaitingHuman(true);
                        context.getWorkflowRun().suspend("Awaiting human review in state: " + currentState.getId());
                        return Uni.createFrom().item(context);
                    }

                    // Determine next state based on transition rules
                    Optional<NodeDefinition> nextState = determineNextState(currentState, result, workflow, context);

                    if (nextState.isEmpty()) {
                        LOG.debugf("No valid transition from state: %s. Workflow complete.", currentState.getId());
                        return Uni.createFrom().item(context);
                    }

                    // Transition to next state
                    LOG.debugf("Transitioning from %s to %s", currentState.getId(), nextState.get().getId());
                    return executeStateMachine(nextState.get(), context, workflow);
                })
                .onFailure().recoverWithUni(th -> {
                    LOG.errorf(th, "State execution failed: %s", currentState.getId());
                    return Uni.createFrom().failure(th);
                });
    }

    /**
     * Execute a single state
     */
    private Uni<NodeExecutionResult> executeSingleState(
            NodeDefinition stateDef,
            ExecutionContext context,
            WorkflowDefinition workflow) {

        String stateId = stateDef.getId();
        if (context.isNodeExecuted(stateId)) {
            return Uni.createFrom().item(context.getNodeResult(stateId));
        }

        NodeContext nodeContext = createNodeContext(stateDef, context, workflow);

        return nodeExecutor.execute(stateDef, nodeContext)
                .onFailure().recoverWithUni(th -> {
                    LOG.errorf(th, "State execution failed: %s", stateId);
                    return Uni.createFrom().item(
                            NodeExecutionResult.error(stateId,
                                    tech.kayys.wayang.schema.execution.ErrorPayload.builder()
                                            .message(th.getMessage())
                                            .build()));
                });
    }

    /**
     * Find the initial state in the workflow
     */
    private Optional<NodeDefinition> findInitialState(WorkflowDefinition workflow) {
        return workflow.getNodes().stream()
                .filter(node -> {
                    Map<String, String> props = getNodeProperties(node);
                    return "true".equals(props.get("initialState"));
                })
                .findFirst();
    }

    /**
     * Check if a state is a final state
     */
    private boolean isFinalState(NodeDefinition state, WorkflowDefinition workflow) {
        Map<String, String> props = getNodeProperties(state);
        return "true".equals(props.get("finalState"));
    }

    /**
     * Determine the next state based on transition rules and current execution
     * result
     */
    private Optional<NodeDefinition> determineNextState(
            NodeDefinition currentState,
            NodeExecutionResult result,
            WorkflowDefinition workflow,
            ExecutionContext context) {

        // Find valid transitions from current state
        return workflow.getEdges().stream()
                .filter(edge -> edge.getFrom().equals(currentState.getId()))
                .filter(edge -> evaluateTransitionGuard(edge, result, context))
                .findFirst()
                .flatMap(edge -> workflow.getNodes().stream()
                        .filter(node -> node.getId().equals(edge.getTo()))
                        .findFirst());
    }

    /**
     * Evaluate if a transition guard condition is met
     */
    private boolean evaluateTransitionGuard(
            tech.kayys.wayang.schema.node.EdgeDefinition edge,
            NodeExecutionResult result,
            ExecutionContext context) {

        // Default: allow transition if previous state succeeded
        if (result.isSuccess()) {
            return true;
        }

        // Check if there's an error transition
        Map<String, String> edgeProps = getEdgeProperties(edge);
        return "error".equals(edgeProps.get("transitionType")) && !result.isSuccess();
    }

    /**
     * Get node properties as a map
     */
    private Map<String, String> getNodeProperties(NodeDefinition node) {
        if (node.getProperties() == null) {
            return new HashMap<>();
        }
        Map<String, String> props = new HashMap<>();
        node.getProperties().forEach(prop -> {
            if (prop.getDefault() != null) {
                props.put(prop.getName(), String.valueOf(prop.getDefault()));
            }
        });
        return props;
    }

    /**
     * Get edge properties as a map
     */
    private Map<String, String> getEdgeProperties(tech.kayys.wayang.schema.node.EdgeDefinition edge) {
        // Edge properties would be extracted similarly if available
        return new HashMap<>();
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
                .metadata(new HashMap<>(getNodeProperties(nodeDef)))
                .tags(new HashMap<>())
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
