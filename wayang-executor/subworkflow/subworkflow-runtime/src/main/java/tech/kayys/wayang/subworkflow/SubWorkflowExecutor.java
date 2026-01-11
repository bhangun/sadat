package tech.kayys.silat.executor.subworkflow;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.silat.core.domain.*;
import tech.kayys.silat.core.engine.*;
import tech.kayys.silat.executor.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * ============================================================================
 * SUB-WORKFLOW EXECUTOR
 * ============================================================================
 *
 * Executes child workflows as nodes within parent workflows, enabling:
 * - Workflow composition and reusability
 * - Hierarchical workflow structures
 * - Isolation of complex logic into sub-workflows
 * - Cross-tenant workflow invocation (with security)
 * - Asynchronous sub-workflow execution with monitoring
 *
 * Architecture:
 * - Parent workflow creates sub-workflow run
 * - Parent suspends waiting for sub-workflow completion
 * - Sub-workflow executes independently
 * - Parent resumes when sub-workflow completes
 * - Parent inherits sub-workflow outputs
 *
 * Features:
 * - Input/output mapping between parent and child
 * - Error propagation and compensation
 * - Timeout handling
 * - Cancellation cascade (parent cancels child)
 * - Monitoring and visibility
 * - Multi-tenant support with security
 *
 * @author Silat Team
 * @version 1.0.0
 */
@Executor(
    executorType = "sub-workflow-executor",
    communicationType = tech.kayys.silat.core.scheduler.CommunicationType.GRPC,
    maxConcurrentTasks = 100,
    supportedNodeTypes = {"SUB_WORKFLOW"},
    version = "1.0.0"
)
@ApplicationScoped
public class SubWorkflowExecutor extends AbstractWorkflowExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(SubWorkflowExecutor.class);

    @Inject
    WorkflowRunManager runManager;

    @Inject
    SubWorkflowRegistry subWorkflowRegistry;

    @Inject
    SubWorkflowMonitor monitor;

    @Inject
    SubWorkflowMapper inputOutputMapper;

    @Inject
    SubWorkflowSecurityValidator securityValidator;

    @ConfigProperty(name = "silat.subworkflow.default-timeout-seconds", defaultValue = "3600")
    long defaultTimeoutSeconds;

    @ConfigProperty(name = "silat.subworkflow.max-depth", defaultValue = "10")
    int maxNestingDepth;

    @ConfigProperty(name = "silat.subworkflow.enable-cross-tenant", defaultValue = "false")
    boolean enableCrossTenant;

    // Track parent-child relationships
    private final Map<WorkflowRunId, SubWorkflowExecution> activeSubWorkflows =
        new ConcurrentHashMap<>();

    // ==================== MAIN EXECUTION ====================

    @Override
    public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
        LOG.info("Executing sub-workflow node: parent={}, node={}",
            task.runId().value(), task.nodeId().value());

        return extractSubWorkflowConfig(task)
            .flatMap(config -> validateExecution(task, config))
            .flatMap(config -> prepareInputs(task, config))
            .flatMap(execution -> launchSubWorkflow(task, execution))
            .flatMap(execution -> monitorSubWorkflow(task, execution))
            .map(execution -> mapOutputs(task, execution))
            .onFailure().recoverWithItem(error ->
                handleSubWorkflowError(task, error));
    }

    // ==================== CONFIGURATION EXTRACTION ====================

    /**
     * Extract sub-workflow configuration from node config
     */
    private Uni<SubWorkflowConfig> extractSubWorkflowConfig(NodeExecutionTask task) {
        return Uni.createFrom().item(() -> {
            Map<String, Object> nodeConfig = task.context();

            // Required: Sub-workflow definition ID or name
            String workflowId = (String) nodeConfig.get("subWorkflowId");
            String workflowName = (String) nodeConfig.get("subWorkflowName");

            if (workflowId == null && workflowName == null) {
                throw new IllegalArgumentException(
                    "Either 'subWorkflowId' or 'subWorkflowName' must be specified");
            }

            // Optional: Input mapping configuration
            @SuppressWarnings("unchecked")
            Map<String, String> inputMapping =
                (Map<String, String>) nodeConfig.getOrDefault("inputMapping", Map.of());

            // Optional: Output mapping configuration
            @SuppressWarnings("unchecked")
            Map<String, String> outputMapping =
                (Map<String, String>) nodeConfig.getOrDefault("outputMapping", Map.of());

            // Optional: Execution options
            boolean waitForCompletion =
                (boolean) nodeConfig.getOrDefault("waitForCompletion", true);

            long timeoutSeconds = nodeConfig.containsKey("timeoutSeconds") ?
                ((Number) nodeConfig.get("timeoutSeconds")).longValue() :
                defaultTimeoutSeconds;

            // Optional: Target tenant (for cross-tenant invocation)
            String targetTenantId = (String) nodeConfig.get("targetTenantId");

            // Optional: Error handling strategy
            String errorStrategy = (String) nodeConfig.getOrDefault(
                "errorStrategy", "PROPAGATE");

            // Optional: Compensation strategy
            boolean enableCompensation =
                (boolean) nodeConfig.getOrDefault("enableCompensation", true);

            // Optional: Pass-through context
            boolean passThroughContext =
                (boolean) nodeConfig.getOrDefault("passThroughContext", false);

            // Optional: Labels to apply to sub-workflow
            @SuppressWarnings("unchecked")
            Map<String, String> labels =
                (Map<String, String>) nodeConfig.getOrDefault("labels", Map.of());

            return new SubWorkflowConfig(
                workflowId,
                workflowName,
                inputMapping,
                outputMapping,
                waitForCompletion,
                Duration.ofSeconds(timeoutSeconds),
                targetTenantId,
                SubWorkflowErrorStrategy.valueOf(errorStrategy),
                enableCompensation,
                passThroughContext,
                labels
            );
        });
    }

    // ==================== VALIDATION ====================

    /**
     * Validate sub-workflow execution
     */
    private Uni<SubWorkflowConfig> validateExecution(
            NodeExecutionTask task,
            SubWorkflowConfig config) {

        return Uni.createFrom().item(() -> {
            // Check nesting depth
            int currentDepth = getCurrentNestingDepth(task);
            if (currentDepth >= maxNestingDepth) {
                throw new SubWorkflowException(
                    "MAX_NESTING_DEPTH_EXCEEDED",
                    String.format("Maximum nesting depth of %d exceeded (current: %d)",
                        maxNestingDepth, currentDepth)
                );
            }

            // Validate cross-tenant invocation
            if (config.targetTenantId() != null) {
                if (!enableCrossTenant) {
                    throw new SubWorkflowException(
                        "CROSS_TENANT_DISABLED",
                        "Cross-tenant sub-workflow invocation is disabled"
                    );
                }

                // Validate security permissions
                securityValidator.validateCrossTenantAccess(
                    extractParentTenantId(task),
                    config.targetTenantId()
                );
            }

            return config;
        })
        .flatMap(validConfig ->
            // Verify sub-workflow exists
            subWorkflowRegistry.verifyWorkflowExists(
                validConfig.workflowId() != null ?
                    validConfig.workflowId() : validConfig.workflowName(),
                validConfig.targetTenantId()
            ).replaceWith(validConfig)
        );
    }

    /**
     * Calculate current nesting depth
     */
    private int getCurrentNestingDepth(NodeExecutionTask task) {
        // Extract from parent context if available
        Object depthObj = task.context().get("_nestingDepth");
        return depthObj != null ? ((Number) depthObj).intValue() : 0;
    }

    /**
     * Extract parent tenant ID from task context
     */
    private String extractParentTenantId(NodeExecutionTask task) {
        return (String) task.context().get("_tenantId");
    }

    // ==================== INPUT PREPARATION ====================

    /**
     * Prepare inputs for sub-workflow
     */
    private Uni<SubWorkflowExecution> prepareInputs(
            NodeExecutionTask task,
            SubWorkflowConfig config) {

        return Uni.createFrom().item(() -> {
            Map<String, Object> parentContext = task.context();

            // Apply input mapping
            Map<String, Object> childInputs = config.passThroughContext() ?
                new HashMap<>(parentContext) :
                inputOutputMapper.mapInputs(parentContext, config.inputMapping());

            // Add metadata
            childInputs.put("_parentRunId", task.runId().value());
            childInputs.put("_parentNodeId", task.nodeId().value());
            childInputs.put("_nestingDepth", getCurrentNestingDepth(task) + 1);

            // Determine target tenant
            String targetTenantId = config.targetTenantId() != null ?
                config.targetTenantId() :
                extractParentTenantId(task);

            childInputs.put("_tenantId", targetTenantId);

            // Create execution context
            return new SubWorkflowExecution(
                task.runId(),
                task.nodeId(),
                null, // Will be set after launch
                config,
                childInputs,
                targetTenantId,
                Instant.now(),
                SubWorkflowStatus.INITIALIZING
            );
        });
    }

    // ==================== SUB-WORKFLOW LAUNCH ====================

    /**
     * Launch the sub-workflow
     */
    private Uni<SubWorkflowExecution> launchSubWorkflow(
            NodeExecutionTask task,
            SubWorkflowExecution execution) {

        LOG.info("Launching sub-workflow: parent={}, workflowId={}",
            task.runId().value(),
            execution.config().workflowId());

        // Create labels for sub-workflow
        Map<String, String> labels = new HashMap<>(execution.config().labels());
        labels.put("parentRunId", execution.parentRunId().value());
        labels.put("parentNodeId", execution.parentNodeId().value());
        labels.put("executionType", "sub-workflow");

        // Create run request
        tech.kayys.silat.api.dto.CreateRunRequest request =
            new tech.kayys.silat.api.dto.CreateRunRequest(
                execution.config().workflowId() != null ?
                    execution.config().workflowId() :
                    resolveWorkflowId(execution.config().workflowName()),
                execution.childInputs(),
                labels,
                null // No trigger
            );

        // Create and start sub-workflow
        return runManager.createRun(
                request,
                TenantId.of(execution.targetTenantId()))
            .flatMap(run -> {
                // Update execution with child run ID
                SubWorkflowExecution updated = execution.withChildRunId(run.getId());

                // Register parent-child relationship
                activeSubWorkflows.put(run.getId(), updated);

                // Start the run
                return runManager.startRun(run.getId(), TenantId.of(execution.targetTenantId()))
                    .map(startedRun -> updated.withStatus(SubWorkflowStatus.RUNNING));
            })
            .onFailure().invoke(error -> {
                LOG.error("Failed to launch sub-workflow", error);
                activeSubWorkflows.remove(execution.childRunId());
            });
    }

    /**
     * Resolve workflow name to ID
     */
    private String resolveWorkflowId(String workflowName) {
        return subWorkflowRegistry.resolveWorkflowId(workflowName);
    }

    // ==================== MONITORING ====================

    /**
     * Monitor sub-workflow execution
     */
    private Uni<SubWorkflowExecution> monitorSubWorkflow(
            NodeExecutionTask task,
            SubWorkflowExecution execution) {

        if (!execution.config().waitForCompletion()) {
            // Fire-and-forget mode
            LOG.info("Sub-workflow launched in fire-and-forget mode: {}",
                execution.childRunId().value());
            return Uni.createFrom().item(
                execution.withStatus(SubWorkflowStatus.DETACHED));
        }

        // Wait for completion with timeout
        return monitor.waitForCompletion(
                execution.childRunId(),
                TenantId.of(execution.targetTenantId()),
                execution.config().timeout())
            .map(result -> execution.withResult(result))
            .onFailure(TimeoutException.class).recoverWithItem(error -> {
                LOG.error("Sub-workflow timed out: {}", execution.childRunId().value());

                // Cancel timed-out sub-workflow
                runManager.cancelRun(
                    execution.childRunId(),
                    TenantId.of(execution.targetTenantId()),
                    "Parent node timeout"
                ).subscribe().with(
                    v -> LOG.info("Cancelled timed-out sub-workflow"),
                    err -> LOG.error("Failed to cancel sub-workflow", err)
                );

                return execution.withStatus(SubWorkflowStatus.TIMEOUT)
                    .withError(new ErrorInfo(
                        "SUB_WORKFLOW_TIMEOUT",
                        "Sub-workflow execution timed out after " +
                            execution.config().timeout().getSeconds() + " seconds",
                        "",
                        Map.of("childRunId", execution.childRunId().value())
                    ));
            })
            .eventually(() -> {
                // Clean up
                activeSubWorkflows.remove(execution.childRunId());
            });
    }

    // ==================== OUTPUT MAPPING ====================

    /**
     * Map outputs from sub-workflow to parent
     */
    private NodeExecutionResult mapOutputs(
            NodeExecutionTask task,
            SubWorkflowExecution execution) {

        if (execution.status() == SubWorkflowStatus.DETACHED) {
            // Fire-and-forget: return immediately
            return NodeExecutionResult.success(
                task.runId(),
                task.nodeId(),
                task.attempt(),
                Map.of(
                    "childRunId", execution.childRunId().value(),
                    "status", "DETACHED",
                    "launchedAt", execution.startedAt().toString()
                ),
                task.token()
            );
        }

        if (execution.status() == SubWorkflowStatus.TIMEOUT) {
            return handleTimeout(task, execution);
        }

        SubWorkflowResult result = execution.result();

        if (result.status() == RunStatus.COMPLETED) {
            // Success: map outputs
            Map<String, Object> parentOutputs = inputOutputMapper.mapOutputs(
                result.outputs(),
                execution.config().outputMapping()
            );

            // Add metadata
            parentOutputs.put("childRunId", execution.childRunId().value());
            parentOutputs.put("childStatus", result.status().name());
            parentOutputs.put("childDuration", result.duration().toMillis());

            LOG.info("Sub-workflow completed successfully: childRunId={}, duration={}ms",
                execution.childRunId().value(), result.duration().toMillis());

            return NodeExecutionResult.success(
                task.runId(),
                task.nodeId(),
                task.attempt(),
                parentOutputs,
                task.token()
            );

        } else {
            // Failure: handle based on error strategy
            return handleSubWorkflowFailure(task, execution, result);
        }
    }

    // ==================== ERROR HANDLING ====================

    /**
     * Handle sub-workflow failure
     */
    private NodeExecutionResult handleSubWorkflowFailure(
            NodeExecutionTask task,
            SubWorkflowExecution execution,
            SubWorkflowResult result) {

        LOG.error("Sub-workflow failed: childRunId={}, status={}",
            execution.childRunId().value(), result.status());

        ErrorInfo childError = result.error() != null ? result.error() :
            new ErrorInfo(
                "SUB_WORKFLOW_FAILED",
                "Sub-workflow failed with status: " + result.status(),
                "",
                Map.of("childRunId", execution.childRunId().value())
            );

        return switch (execution.config().errorStrategy()) {
            case PROPAGATE -> {
                // Propagate error to parent
                LOG.info("Propagating sub-workflow error to parent");
                yield NodeExecutionResult.failure(
                    task.runId(),
                    task.nodeId(),
                    task.attempt(),
                    new ErrorInfo(
                        "SUB_WORKFLOW_ERROR",
                        "Sub-workflow execution failed: " + childError.message(),
                        childError.stackTrace(),
                        Map.of(
                            "childRunId", execution.childRunId().value(),
                            "childError", childError
                        )
                    ),
                    task.token()
                );
            }

            case IGNORE -> {
                // Ignore error, return success with error details
                LOG.info("Ignoring sub-workflow error");
                yield NodeExecutionResult.success(
                    task.runId(),
                    task.nodeId(),
                    task.attempt(),
                    Map.of(
                        "childRunId", execution.childRunId().value(),
                        "childStatus", result.status().name(),
                        "childError", childError,
                        "errorIgnored", true
                    ),
                    task.token()
                );
            }

            case RETRY_SUB_WORKFLOW -> {
                // This would trigger a retry at the parent level
                LOG.info("Requesting retry of sub-workflow");
                yield NodeExecutionResult.failure(
                    task.runId(),
                    task.nodeId(),
                    task.attempt(),
                    new ErrorInfo(
                        "SUB_WORKFLOW_RETRY_REQUESTED",
                        "Sub-workflow failed, retry requested",
                        "",
                        Map.of("childRunId", execution.childRunId().value())
                    ),
                    task.token()
                );
            }

            case CUSTOM -> {
                // Custom error handling (could call a callback)
                LOG.info("Using custom error handling");
                yield handleCustomError(task, execution, childError);
            }
        };
    }

    /**
     * Handle timeout
     */
    private NodeExecutionResult handleTimeout(
            NodeExecutionTask task,
            SubWorkflowExecution execution) {

        return NodeExecutionResult.failure(
            task.runId(),
            task.nodeId(),
            task.attempt(),
            execution.error(),
            task.token()
        );
    }

    /**
     * Handle custom error strategy
     */
    private NodeExecutionResult handleCustomError(
            NodeExecutionTask task,
            SubWorkflowExecution execution,
            ErrorInfo childError) {

        // This could be extended to call a custom error handler
        // For now, treat as PROPAGATE
        return NodeExecutionResult.failure(
            task.runId(),
            task.nodeId(),
            task.attempt(),
            childError,
            task.token()
        );
    }

    /**
     * Generic error handler
     */
    private NodeExecutionResult handleSubWorkflowError(
            NodeExecutionTask task,
            Throwable error) {

        LOG.error("Sub-workflow execution error", error);

        return NodeExecutionResult.failure(
            task.runId(),
            task.nodeId(),
            task.attempt(),
            new ErrorInfo(
                "SUB_WORKFLOW_EXECUTION_ERROR",
                "Failed to execute sub-workflow: " + error.getMessage(),
                getStackTrace(error),
                Map.of()
            ),
            task.token()
        );
    }

    // ==================== LIFECYCLE HOOKS ====================

    @Override
    public Uni<Void> beforeExecute(NodeExecutionTask task) {
        LOG.debug("Preparing sub-workflow execution: parent={}, node={}",
            task.runId().value(), task.nodeId().value());
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<Void> afterExecute(
            NodeExecutionTask task,
            NodeExecutionResult result) {

        LOG.debug("Sub-workflow execution completed: parent={}, node={}, status={}",
            task.runId().value(), task.nodeId().value(), result.status());

        // Clean up any remaining state
        return Uni.createFrom().voidItem();
    }

    // ==================== CANCELLATION HANDLING ====================

    /**
     * Handle parent workflow cancellation
     */
    public Uni<Void> handleParentCancellation(WorkflowRunId parentRunId) {
        LOG.info("Parent workflow cancelled, cancelling child workflows: parent={}",
            parentRunId.value());

        // Find all child workflows for this parent
        List<SubWorkflowExecution> children = activeSubWorkflows.values().stream()
            .filter(exec -> exec.parentRunId().equals(parentRunId))
            .toList();

        // Cancel all children
        List<Uni<Void>> cancellations = children.stream()
            .map(child ->
                runManager.cancelRun(
                    child.childRunId(),
                    TenantId.of(child.targetTenantId()),
                    "Parent workflow cancelled"
                )
                .onItem().invoke(() ->
                    activeSubWorkflows.remove(child.childRunId())
                )
            )
            .toList();

        return Uni.join().all(cancellations).andFailFast()
            .replaceWithVoid();
    }

    // ==================== UTILITIES ====================

    private String getStackTrace(Throwable error) {
        java.io.StringWriter sw = new java.io.StringWriter();
        error.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }
}