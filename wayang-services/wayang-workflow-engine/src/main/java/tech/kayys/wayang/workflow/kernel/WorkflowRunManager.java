package tech.kayys.wayang.workflow.kernel;

import io.smallrye.mutiny.Uni;

import java.util.Map;

/**
 * 🔒 THE SOLE AUTHORITY for workflow state.
 * Kernel Interface (Locked contract)
 * Non-negotiable rules:
 * 1. ONLY implements state transitions
 * 2. NEVER executes nodes directly
 * 3. OWNS all persistence
 * 4. OWNS all scheduling
 * 5. OWNS all retry logic
 * 
 * Primary Role: Run Lifecycle Manager
 * Responsibilities:
 * 
 * Run persistence (CRUD operations)
 * State transitions and validation
 * Event sourcing and event replay
 * Distributed locking for concurrency control
 * Snapshot management
 * Querying and retrieval operations
 * Tenant isolation and security enforcement
 */
public interface WorkflowRunManager {
        // ==================== CORE ORCHESTRATION ====================

        /**
         * Create a new workflow run with initial context.
         */
        Uni<tech.kayys.wayang.workflow.domain.WorkflowRun> createRun(
                        tech.kayys.wayang.workflow.api.dto.CreateRunRequest request,
                        String tenantId);

        /**
         * Get current snapshot of a workflow run.
         */
        Uni<tech.kayys.wayang.workflow.domain.WorkflowRun> getRun(String runId);

        /**
         * Start executing a created run.
         */
        Uni<tech.kayys.wayang.workflow.domain.WorkflowRun> startRun(String runId, String tenantId);

        /**
         * Suspend a run.
         */
        Uni<tech.kayys.wayang.workflow.domain.WorkflowRun> suspendRun(
                        String runId,
                        String tenantId,
                        String reason,
                        String humanTaskId);

        /**
         * Resume a run.
         */
        Uni<tech.kayys.wayang.workflow.domain.WorkflowRun> resumeRun(
                        String runId,
                        String tenantId,
                        String humanTaskId,
                        Map<String, Object> resumeData);

        /**
         * Cancel a workflow run.
         */
        Uni<Void> cancelRun(String runId, String tenantId, String reason);

        /**
         * Complete a run.
         */
        Uni<tech.kayys.wayang.workflow.domain.WorkflowRun> completeRun(
                        String runId,
                        String tenantId,
                        Map<String, Object> outputs);

        /**
         * Fail a run.
         */
        Uni<tech.kayys.wayang.workflow.domain.WorkflowRun> failRun(
                        String runId,
                        String tenantId,
                        tech.kayys.wayang.workflow.api.dto.ErrorResponse error);

        /**
         * Query runs.
         */
        Uni<java.util.List<tech.kayys.wayang.workflow.api.dto.RunResponse>> queryRuns(
                        String tenantId,
                        String workflowId,
                        tech.kayys.wayang.workflow.api.model.RunStatus status,
                        int page,
                        int size);

        /**
         * Get active runs count.
         */
        Uni<Long> getActiveRunsCount(String tenantId);

        /**
         * Handle the result of a node execution.
         * This is the primary feedback loop from executors.
         */
        Uni<Void> handleNodeResult(
                        WorkflowRunId runId,
                        NodeExecutionResult result);

        /**
         * Signal a waiting workflow run to resume.
         * Used for: human approvals, external callbacks, timer expirations.
         */
        Uni<Void> signal(
                        WorkflowRunId runId,
                        Signal signal);

        // ==================== QUERY & INSPECTION ====================

        /**
         * Get current snapshot of a workflow run.
         */
        Uni<WorkflowRunSnapshot> getSnapshot(WorkflowRunId runId);

        /**
         * Get execution history for debugging/replay.
         */
        Uni<ExecutionHistory> getExecutionHistory(WorkflowRunId runId);

        /**
         * Validate if a state transition is possible.
         */
        Uni<tech.kayys.wayang.sdk.util.WorkflowValidator.ValidationResult> validateTransition(
                        WorkflowRunId runId,
                        WorkflowRunState targetState);

        // ==================== EXTERNAL INTEGRATION ====================

        /**
         * Called by external executors when node execution completes
         */
        Uni<Void> onNodeExecutionCompleted(
                        NodeExecutionResult result,
                        String executorSignature);

        /**
         * Called by external services to signal waiting workflows
         */
        Uni<Void> onExternalSignal(
                        WorkflowRunId runId,
                        ExternalSignal signal,
                        String callbackToken);

        /**
         * Register an external callback for waiting workflows
         */
        Uni<CallbackRegistration> registerCallback(
                        WorkflowRunId runId,
                        String nodeId,
                        CallbackConfig config);

        // ==================== TOKENS ====================

        Uni<ExecutionToken> createExecutionToken(
                        WorkflowRunId runId,
                        String nodeId,
                        int attempt);
}
