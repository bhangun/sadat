package tech.kayys.wayang.orchestrator.engine;

import tech.kayys.wayang.common.domain.*;
import tech.kayys.wayang.orchestrator.client.ExecutorClient;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class DAGWalker {
    
    @Inject
    @RestClient
    ExecutorClient executorClient;
    
    @Inject
    StateManager stateManager;
    
    public Uni<ExecutionRun> walk(ExecutionPlan plan, ExecutionRun run) {
        // Find root nodes (no dependencies)
        List<NodeDescriptor> rootNodes = plan.nodes().stream()
            .filter(node -> node.dependsOn().isEmpty())
            .toList();
        
        // Execute root nodes in parallel
        return Multi.createFrom().iterable(rootNodes)
            .onItem().transformToUniAndMerge(node -> 
                executeNode(run.runId(), node)
            )
            .collect().asList()
            .flatMap(results -> continueExecution(run.runId()));
    }
    
    public Uni<ExecutionRun> continueExecution(String runId) {
        return stateManager.getExecutionRun(runId)
            .flatMap(run -> {
                // Find next executable nodes
                List<NodeDescriptor> readyNodes = findReadyNodes(run);
                
                if (readyNodes.isEmpty()) {
                    // Execution complete
                    return stateManager.completeRun(runId);
                }
                Execute ready nodes
                return Multi.createFrom().iterable(readyNodes)
                    .onItem().transformToUniAndMerge(node -> 
                        executeNode(runId, node)
                    )
                    .collect().asList()
                    .replaceWith(run);
            });
    }
    
    private Uni<ExecutionResult> executeNode(String runId, NodeDescriptor node) {
        return stateManager.markNodeRunning(runId, node.id())
            .flatMap(state -> {
                NodeContext context = buildContext(runId, node, state);
                
                return executorClient.executeNode(context)
                    .flatMap(result -> 
                        stateManager.updateNodeResult(runId, node.id(), result)
                            .replaceWith(result)
                    );
            });
    }
    
    private List<NodeDescriptor> findReadyNodes(ExecutionRun run) {
        return run.plan().nodes().stream()
            .filter(node -> {
                // Check if all dependencies completed successfully
                return node.dependsOn().stream()
                    .allMatch(depId -> {
                        NodeState state = run.nodeStates().get(depId);
                        return state != null && 
                               state.status() == NodeStatus.SUCCEEDED;
                    });
            })
            .filter(node -> {
                // Check if not already executed
                NodeState state = run.nodeStates().get(node.id());
                return state == null || state.status() == NodeStatus.PENDING;
            })
            .toList();
    }
    
    private NodeContext buildContext(String runId, NodeDescriptor node, NodeState state) {
        // Build inputs from predecessor outputs
        Map<String, Object> inputs = new HashMap<>();
        
        node.dependsOn().forEach(depId -> {
            NodeState depState = state.predecessorOutputs().get(depId);
            if (depState != null && depState.result() != null) {
                inputs.putAll(depState.result().outputs());
            }
        });
        
        return new NodeContext(
            runId,
            node.id(),
            state.tenantId(),
            inputs,
            state.variables(),
            state.metadata()
        );
    }
}