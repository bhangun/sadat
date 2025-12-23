package tech.kayys.wayang.workflow.service;

import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.workflow.model.ExecutionContext;
import tech.kayys.wayang.workflow.repository.WorkflowRunRepository;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * DefaultStateStore - Default implementation of StateStore using database persistence
 * 
 * This implementation uses the existing WorkflowRunRepository to persist state
 * to a database, providing reliable storage for workflow executions.
 */
@ApplicationScoped
public class DefaultStateStore implements StateStore {

    @Inject
    WorkflowRunRepository runRepository;

    @Override
    public Uni<WorkflowRun> save(WorkflowRun run) {
        return runRepository.save(run);
    }

    @Override
    public Uni<WorkflowRun> get(String runId) {
        return runRepository.findById(runId);
    }

    @Override
    public Uni<WorkflowRun> update(WorkflowRun run) {
        return runRepository.update(run);
    }

    @Override
    public Uni<Void> saveContext(ExecutionContext context, String runId) {
        // For now, save context data as part of the workflow run
        // In a real implementation, this might save to a separate context table
        return get(runId)
            .onItem().transformToUni(run -> {
                run.updateWorkflowState(context.getAllVariables());
                return save(run);
            })
            .replaceWithVoid();
    }

    @Override
    public Uni<ExecutionContext> getContext(String runId) {
        // For now, return a basic context with the run's inputs and outputs
        // In a real implementation, this would load from a dedicated context table
        return get(runId)
            .map(run -> {
                ExecutionContext context = new ExecutionContext();
                context.setExecutionId(runId);
                context.setTenantId(run.getTenantId());
                context.setInput(run.getInputs());
                // Add any other relevant context data
                return context;
            });
    }

    @Override
    public Uni<Void> saveNodeState(String nodeId, String runId, Map<String, Object> state) {
        return get(runId)
            .onItem().transformToUni(run -> {
                run.recordNodeState(nodeId, createNodeExecutionState(state));
                return save(run);
            })
            .replaceWithVoid();
    }

    @Override
    public Uni<Map<String, Object>> getNodeState(String nodeId, String runId) {
        return get(runId)
            .map(run -> {
                if (run.getNodeStates() != null && run.getNodeStates().containsKey(nodeId)) {
                    // In a real implementation, this would return the actual node state
                    // For now, return an empty map
                    return new HashMap<>();
                }
                return new HashMap<>();
            });
    }

    @Override
    public Uni<Void> saveCheckpoint(String runId, Map<String, Object> checkpointData) {
        return get(runId)
            .onItem().transformToUni(run -> {
                run.setCheckpointData(checkpointData);
                return save(run);
            })
            .replaceWithVoid();
    }

    @Override
    public Uni<Map<String, Object>> getCheckpoint(String runId) {
        return get(runId)
            .map(run -> run.getCheckpointData() != null ? run.getCheckpointData() : new HashMap<>());
    }

    @Override
    public Uni<List<WorkflowRun>> listByTenant(String tenantId, int offset, int limit) {
        // Use the existing method with pagination
        // Since there's no exact method, let's use the find method with pagination
        return runRepository.find("tenantId = ?1", Sort.descending("createdAt"), tenantId)
            .page(offset, limit)
            .list();
    }

    @Override
    public Uni<Void> delete(String runId) {
        return runRepository.deleteById(runId).replaceWithVoid();
    }

    /**
     * Helper method to create a NodeExecutionState from a map
     */
    private tech.kayys.wayang.sdk.dto.NodeExecutionState createNodeExecutionState(Map<String, Object> state) {
        // This is a simplified implementation
        // In a real implementation, this would properly map the state to NodeExecutionState
        String status = (String) state.getOrDefault("status", "PENDING");
        tech.kayys.wayang.sdk.dto.NodeExecutionState.NodeStatus nodeStatus =
            tech.kayys.wayang.sdk.dto.NodeExecutionState.NodeStatus.valueOf(status);

        return new tech.kayys.wayang.sdk.dto.NodeExecutionState(
            (String) state.get("nodeId"),
            nodeStatus,
            (Map<String, Object>) state.get("inputs"),
            (Map<String, Object>) state.get("outputs"),
            (String) state.get("errorMessage"),
            java.time.Instant.now(),
            java.time.Instant.now()
        );
    }
}