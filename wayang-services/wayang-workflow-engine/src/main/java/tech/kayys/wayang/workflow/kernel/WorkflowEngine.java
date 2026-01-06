package tech.kayys.wayang.workflow.kernel;

import java.util.List;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.sdk.dto.ExecutionMetrics;
import tech.kayys.wayang.sdk.util.WorkflowValidator.ValidationResult;

/**
 * 🔒 PURE STATELESS execution engine.
 * 
 * Non-negotiable rules:
 * 1. Executes EXACTLY ONE node
 * 2. NO persistence
 * 3. NO retry logic
 * 4. NO workflow awareness
 * 5. ALWAYS deterministic (same inputs → same outputs)
 * 
 * Primary Role: Orchestration Controller
 * Responsibilities:
 * 
 * Workflow definition validation and selection
 * Execution strategy selection (DAG, sequential, parallel, etc.)
 * High-level execution coordination
 * Error handling and recovery strategies
 * Integration with external systems (Policy Engine, Telemetry, Registry)
 * Workflow trigger handling (API, events, schedules)
 */
public interface WorkflowEngine {

        /**
         * Execute a single node with the given context.
         * 
         * @param context Current execution context (variables, metadata)
         * @param node    Node to execute (semantic descriptor)
         * @param token   Security token for replay protection
         * @return Structured result with status and optional updates
         */
        Uni<NodeExecutionResult> executeNode(
                        ExecutionContext context,
                        NodeDescriptor node,
                        ExecutionToken token);

        Uni<BatchExecutionResult> executeNodes(
                        List<NodeExecutionRequest> requests,
                        ExecutionContext sharedContext);

        // Metrics collection
        ExecutionMetrics collectMetrics();

        // Resource estimation
        ResourceEstimate estimateResources(
                        ExecutionContext context,
                        NodeDescriptor node);

        /**
         * Dry-run execution (no side effects).
         * Used for validation, simulation, and planning.
         */
        Uni<NodeExecutionResult> dryRunNode(
                        ExecutionContext context,
                        NodeDescriptor node);

        /**
         * Replay execution with historical context.
         * Ensures idempotency and supports debugging.
         */
        Uni<NodeExecutionResult> replayNode(
                        ExecutionContext historicalContext,
                        NodeDescriptor node,
                        ExecutionToken originalToken,
                        ReplayOptions options);

        /**
         * Validate if execution is possible.
         * Checks prerequisites without executing.
         */
        Uni<ValidationResult> validateExecution(
                        ExecutionContext context,
                        NodeDescriptor node);
}
