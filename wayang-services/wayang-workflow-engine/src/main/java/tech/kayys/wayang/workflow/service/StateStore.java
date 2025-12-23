package tech.kayys.wayang.workflow.service;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.workflow.model.ExecutionContext;

import java.util.List;
import java.util.Map;

/**
 * StateStore - Interface for storing and retrieving workflow execution state
 * 
 * This interface provides a generic contract for state persistence that can be
 * implemented by different storage backends (database, memory, distributed
 * store, etc.)
 * making the workflow engine more use case agnostic.
 */
public interface StateStore {

    /**
     * Save a workflow run
     * 
     * @param run The workflow run to save
     * @return A Uni containing the saved run
     */
    Uni<WorkflowRun> save(WorkflowRun run);

    /**
     * Get a workflow run by ID
     * 
     * @param runId The run ID to retrieve
     * @return A Uni containing the workflow run
     */
    Uni<WorkflowRun> get(String runId);

    /**
     * Load a workflow run by ID (alias for get)
     * 
     * @param runId The run ID to load
     * @return A Uni containing the workflow run
     */
    default Uni<WorkflowRun> load(String runId) {
        return get(runId);
    }

    /**
     * Update a workflow run
     * 
     * @param run The workflow run to update
     * @return A Uni containing the updated run
     */
    Uni<WorkflowRun> update(WorkflowRun run);

    /**
     * Save execution context
     * 
     * @param context The execution context to save
     * @param runId   The run ID associated with the context
     * @return A Uni indicating completion
     */
    Uni<Void> saveContext(ExecutionContext context, String runId);

    /**
     * Get execution context
     * 
     * @param runId The run ID to get context for
     * @return A Uni containing the execution context
     */
    Uni<ExecutionContext> getContext(String runId);

    /**
     * Save node execution state
     * 
     * @param nodeId The node ID
     * @param runId  The run ID
     * @param state  The state to save
     * @return A Uni indicating completion
     */
    Uni<Void> saveNodeState(String nodeId, String runId, Map<String, Object> state);

    /**
     * Get node execution state
     * 
     * @param nodeId The node ID
     * @param runId  The run ID
     * @return A Uni containing the node state
     */
    Uni<Map<String, Object>> getNodeState(String nodeId, String runId);

    /**
     * Save execution checkpoint
     * 
     * @param runId          The run ID
     * @param checkpointData The checkpoint data
     * @return A Uni indicating completion
     */
    Uni<Void> saveCheckpoint(String runId, Map<String, Object> checkpointData);

    /**
     * Get execution checkpoint
     * 
     * @param runId The run ID
     * @return A Uni containing the checkpoint data
     */
    Uni<Map<String, Object>> getCheckpoint(String runId);

    /**
     * List workflow runs for a tenant
     * 
     * @param tenantId The tenant ID
     * @param offset   The offset for pagination
     * @param limit    The limit for pagination
     * @return A Uni containing the list of workflow runs
     */
    Uni<List<WorkflowRun>> listByTenant(String tenantId, int offset, int limit);

    /**
     * Delete a workflow run
     * 
     * @param runId The run ID to delete
     * @return A Uni indicating completion
     */
    Uni<Void> delete(String runId);
}