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

// ==================== SUPPORTING CLASSES ====================

/**
 * Sub-workflow configuration
 */
record SubWorkflowConfig(
    String workflowId,
    String workflowName,
    Map<String, String> inputMapping,
    Map<String, String> outputMapping,
    boolean waitForCompletion,
    Duration timeout,
    String targetTenantId,
    SubWorkflowErrorStrategy errorStrategy,
    boolean enableCompensation,
    boolean passThroughContext,
    Map<String, String> labels
) {}

/**
 * Sub-workflow execution tracking
 */
record SubWorkflowExecution(
    WorkflowRunId parentRunId,
    NodeId parentNodeId,
    WorkflowRunId childRunId,
    SubWorkflowConfig config,
    Map<String, Object> childInputs,
    String targetTenantId,
    Instant startedAt,
    SubWorkflowStatus status,
    SubWorkflowResult result,
    ErrorInfo error
) {
    // Constructor with defaults
    SubWorkflowExecution(
            WorkflowRunId parentRunId,
            NodeId parentNodeId,
            WorkflowRunId childRunId,
            SubWorkflowConfig config,
            Map<String, Object> childInputs,
            String targetTenantId,
            Instant startedAt,
            SubWorkflowStatus status) {
        this(parentRunId, parentNodeId, childRunId, config, childInputs,
            targetTenantId, startedAt, status, null, null);
    }
    
    SubWorkflowExecution withChildRunId(WorkflowRunId childRunId) {
        return new SubWorkflowExecution(parentRunId, parentNodeId, childRunId,
            config, childInputs, targetTenantId, startedAt, status, result, error);
    }
    
    SubWorkflowExecution withStatus(SubWorkflowStatus status) {
        return new SubWorkflowExecution(parentRunId, parentNodeId, childRunId,
            config, childInputs, targetTenantId, startedAt, status, result, error);
    }
    
    SubWorkflowExecution withResult(SubWorkflowResult result) {
        return new SubWorkflowExecution(parentRunId, parentNodeId, childRunId,
            config, childInputs, targetTenantId, startedAt, 
            SubWorkflowStatus.COMPLETED, result, error);
    }
    
    SubWorkflowExecution withError(ErrorInfo error) {
        return new SubWorkflowExecution(parentRunId, parentNodeId, childRunId,
            config, childInputs, targetTenantId, startedAt, status, result, error);
    }
}

/**
 * Sub-workflow result
 */
record SubWorkflowResult(
    RunStatus status,
    Map<String, Object> outputs,
    ErrorInfo error,
    Duration duration
) {}

/**
 * Sub-workflow status
 */
enum SubWorkflowStatus {
    INITIALIZING,
    RUNNING,
    COMPLETED,
    FAILED,
    TIMEOUT,
    DETACHED
}

/**
 * Error handling strategies
 */
enum SubWorkflowErrorStrategy {
    PROPAGATE,              // Propagate error to parent (fail parent node)
    IGNORE,                 // Ignore error (parent node succeeds)
    RETRY_SUB_WORKFLOW,     // Retry the sub-workflow
    CUSTOM                  // Custom error handling
}

/**
 * Sub-workflow registry
 */
@ApplicationScoped
class SubWorkflowRegistry {
    
    private static final Logger LOG = LoggerFactory.getLogger(SubWorkflowRegistry.class);
    
    @Inject
    tech.kayys.silat.core.registry.WorkflowDefinitionRegistry definitionRegistry;
    
    /**
     * Verify workflow exists
     */
    public Uni<Boolean> verifyWorkflowExists(String workflowId, String tenantId) {
        return definitionRegistry.getDefinition(
                WorkflowDefinitionId.of(workflowId),
                TenantId.of(tenantId))
            .map(def -> true)
            .onFailure().recoverWithItem(false);
    }
    
    /**
     * Resolve workflow name to ID
     */
    public String resolveWorkflowId(String workflowName) {
        // This would query the registry to find workflow by name
        // For now, assume name == id
        return workflowName;
    }
}

/**
 * Sub-workflow monitor
 */
@ApplicationScoped
class SubWorkflowMonitor {
    
    private static final Logger LOG = LoggerFactory.getLogger(SubWorkflowMonitor.class);
    
    @Inject
    WorkflowRunManager runManager;
    
    /**
     * Wait for sub-workflow completion with timeout
     */
    public Uni<SubWorkflowResult> waitForCompletion(
            WorkflowRunId childRunId,
            TenantId tenantId,
            Duration timeout) {
        
        LOG.debug("Monitoring sub-workflow: childRunId={}, timeout={}s",
            childRunId.value(), timeout.getSeconds());
        
        Instant deadline = Instant.now().plus(timeout);
        
        return pollForCompletion(childRunId, tenantId, deadline);
    }
    
    /**
     * Poll for completion
     */
    private Uni<SubWorkflowResult> pollForCompletion(
            WorkflowRunId childRunId,
            TenantId tenantId,
            Instant deadline) {
        
        return runManager.getRun(childRunId, tenantId)
            .flatMap(run -> {
                if (isTerminal(run.getStatus())) {
                    // Completed
                    return Uni.createFrom().item(createResult(run));
                }
                
                // Check timeout
                if (Instant.now().isAfter(deadline)) {
                    return Uni.createFrom().failure(
                        new TimeoutException("Sub-workflow execution timed out"));
                }
                
                // Continue polling
                return Uni.createFrom().item(0)
                    .onItem().delayIt().by(Duration.ofMillis(500))
                    .flatMap(v -> pollForCompletion(childRunId, tenantId, deadline));
            });
    }
    
    private boolean isTerminal(RunStatus status) {
        return status == RunStatus.COMPLETED ||
               status == RunStatus.FAILED ||
               status == RunStatus.CANCELLED;
    }
    
    private SubWorkflowResult createResult(WorkflowRun run) {
        WorkflowRunSnapshot snapshot = run.createSnapshot();
        
        Duration duration = snapshot.startedAt() != null && snapshot.completedAt() != null ?
            Duration.between(snapshot.startedAt(), snapshot.completedAt()) :
            Duration.ZERO;
        
        ErrorInfo error = null;
        if (snapshot.status() == RunStatus.FAILED) {
            // Extract error from failed nodes
            error = snapshot.nodeExecutions().values().stream()
                .filter(exec -> exec.getLastError() != null)
                .map(tech.kayys.silat.core.domain.NodeExecution::getLastError)
                .findFirst()
                .orElse(new ErrorInfo("UNKNOWN_ERROR", "Sub-workflow failed", "", Map.of()));
        }
        
        return new SubWorkflowResult(
            snapshot.status(),
            snapshot.variables(),
            error,
            duration
        );
    }
}

/**
 * Input/Output mapper
 */
@ApplicationScoped
class SubWorkflowMapper {
    
    private static final Logger LOG = LoggerFactory.getLogger(SubWorkflowMapper.class);
    
    /**
     * Map parent context to child inputs
     */
    public Map<String, Object> mapInputs(
            Map<String, Object> parentContext,
            Map<String, String> mapping) {
        
        Map<String, Object> childInputs = new HashMap<>();
        
        if (mapping.isEmpty()) {
            // No mapping: pass through all (except internal fields)
            parentContext.forEach((key, value) -> {
                if (!key.startsWith("_")) {
                    childInputs.put(key, value);
                }
            });
        } else {
            // Apply mapping: childField -> parentField
            mapping.forEach((childField, parentField) -> {
                Object value = resolveValue(parentContext, parentField);
                if (value != null) {
                    childInputs.put(childField, value);
                }
            });
        }
        
        LOG.debug("Mapped {} parent fields to {} child inputs", 
            parentContext.size(), childInputs.size());
        
        return childInputs;
    }
    
    /**
     * Map child outputs to parent context
     */
    public Map<String, Object> mapOutputs(
            Map<String, Object> childOutputs,
            Map<String, String> mapping) {
        
        Map<String, Object> parentOutputs = new HashMap<>();
        
        if (mapping.isEmpty()) {
            // No mapping: pass through all
            parentOutputs.putAll(childOutputs);
        } else {
            // Apply mapping: parentField -> childField
            mapping.forEach((parentField, childField) -> {
                Object value = resolveValue(childOutputs, childField);
                if (value != null) {
                    parentOutputs.put(parentField, value);
                }
            });
        }
        
        LOG.debug("Mapped {} child outputs to {} parent fields", 
            childOutputs.size(), parentOutputs.size());
        
        return parentOutputs;
    }
    
    /**
     * Resolve value from context using path notation
     * Supports: "field", "object.field", "array[0]", "object.array[1].field"
     */
    private Object resolveValue(Map<String, Object> context, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        
        String[] parts = path.split("\\.");
        Object current = context;
        
        for (String part : parts) {
            if (current == null) {
                return null;
            }
            
            // Handle array indexing
            if (part.contains("[")) {
                int bracketIndex = part.indexOf('[');
                String fieldName = part.substring(0, bracketIndex);
                int arrayIndex = Integer.parseInt(
                    part.substring(bracketIndex + 1, part.indexOf(']')));
                
                if (current instanceof Map) {
                    current = ((Map<?, ?>) current).get(fieldName);
                }
                
                if (current instanceof List) {
                    List<?> list = (List<?>) current;
                    if (arrayIndex >= 0 && arrayIndex < list.size()) {
                        current = list.get(arrayIndex);
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                // Simple field access
                if (current instanceof Map) {
                    current = ((Map<?, ?>) current).get(part);
                } else {
                    return null;
                }
            }
        }
        
        return current;
    }
}

/**
 * Security validator for cross-tenant access
 */
@ApplicationScoped
class SubWorkflowSecurityValidator {
    
    private static final Logger LOG = LoggerFactory.getLogger(SubWorkflowSecurityValidator.class);
    
    @Inject
    tech.kayys.silat.api.security.TenantContext tenantContext;
    
    /**
     * Validate cross-tenant access
     */
    public void validateCrossTenantAccess(String sourceTenantId, String targetTenantId) {
        LOG.debug("Validating cross-tenant access: source={}, target={}", 
            sourceTenantId, targetTenantId);
        
        // Check if source tenant has permission to invoke workflows in target tenant
        // This could check:
        // - Tenant relationships (parent-child, partners)
        // - Access control policies
        // - Service agreements
        
        if (!hasPermission(sourceTenantId, targetTenantId)) {
            throw new SecurityException(
                String.format("Tenant %s does not have permission to access tenant %s",
                    sourceTenantId, targetTenantId));
        }
    }
    
    private boolean hasPermission(String sourceTenantId, String targetTenantId) {
        // In production, this would check actual permissions
        // For now, allow if same tenant
        return sourceTenantId.equals(targetTenantId);
    }
}

/**
 * Sub-workflow exception
 */
class SubWorkflowException extends RuntimeException {
    private final String code;
    
    public SubWorkflowException(String code, String message) {
        super(message);
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
}

// ==================== CONFIGURATION CLASSES ====================

/**
 * Sub-workflow executor configuration properties
 */
@ApplicationScoped
class SubWorkflowExecutorConfig {
    
    @ConfigProperty(name = "silat.subworkflow.enabled", defaultValue = "true")
    boolean enabled;
    
    @ConfigProperty(name = "silat.subworkflow.default-timeout-seconds", defaultValue = "3600")
    long defaultTimeoutSeconds;
    
    @ConfigProperty(name = "silat.subworkflow.max-depth", defaultValue = "10")
    int maxNestingDepth;
    
    @ConfigProperty(name = "silat.subworkflow.enable-cross-tenant", defaultValue = "false")
    boolean enableCrossTenant;
    
    @ConfigProperty(name = "silat.subworkflow.poll-interval-ms", defaultValue = "500")
    long pollIntervalMs;
    
    @ConfigProperty(name = "silat.subworkflow.max-concurrent", defaultValue = "100")
    int maxConcurrent;
    
    public boolean isEnabled() { return enabled; }
    public long getDefaultTimeoutSeconds() { return defaultTimeoutSeconds; }
    public int getMaxNestingDepth() { return maxNestingDepth; }
    public boolean isEnableCrossTenant() { return enableCrossTenant; }
    public long getPollIntervalMs() { return pollIntervalMs; }
    public int getMaxConcurrent() { return maxConcurrent; }
}

// ==================== USAGE EXAMPLE ====================

/**
 * Example: E-Commerce workflow with sub-workflow for fraud detection
 * 
 * ```java
 * // Define fraud detection workflow
 * client.workflows()
 *     .create("fraud-detection")
 *     .version("1.0.0")
 *     .addNode(new NodeDefinitionDto(
 *         "check-customer-history",
 *         "Check Customer History",
 *         "TASK",
 *         "fraud-checker",
 *         Map.of(),
 *         List.of(),
 *         List.of(),
 *         null, 30L, false
 *     ))
 *     .addInput("customerId", new InputDefinitionDto(...))
 *     .addInput("transactionAmount", new InputDefinitionDto(...))
 *     .addOutput("riskScore", new OutputDefinitionDto(...))
 *     .addOutput("approved", new OutputDefinitionDto(...))
 *     .execute()
 *     .await().indefinitely();
 * 
 * // Define main order processing workflow with fraud check sub-workflow
 * client.workflows()
 *     .create("order-processing")
 *     .version("2.0.0")
 *     .addNode(new NodeDefinitionDto(
 *         "validate-order",
 *         "Validate Order",
 *         "TASK",
 *         "order-validator",
 *         Map.of(),
 *         List.of(),
 *         List.of(new TransitionDto("fraud-check", null, "SUCCESS")),
 *         null, 30L, true
 *     ))
 *     .addNode(new NodeDefinitionDto(
 *         "fraud-check",
 *         "Fraud Detection Check",
 *         "SUB_WORKFLOW",
 *         "sub-workflow-executor",
 *         Map.of(
 *             "subWorkflowId", "fraud-detection",
 *             "waitForCompletion", true,
 *             "timeoutSeconds", 60,
 *             "inputMapping", Map.of(
 *                 "customerId", "customerId",
 *                 "transactionAmount", "totalAmount"
 *             ),
 *             "outputMapping", Map.of(
 *                 "fraudRiskScore", "riskScore",
 *                 "fraudApproved", "approved"
 *             ),
 *             "errorStrategy", "PROPAGATE"
 *         ),
 *         List.of("validate-order"),
 *         List.of(
 *             new TransitionDto("process-payment", "fraudApproved == true", "CONDITION"),
 *             new TransitionDto("manual-review", "fraudApproved == false", "CONDITION")
 *         ),
 *         null, 120L, true
 *     ))
 *     .addNode(new NodeDefinitionDto(
 *         "process-payment",
 *         "Process Payment",
 *         "TASK",
 *         "payment-processor",
 *         Map.of(),
 *         List.of("fraud-check"),
 *         List.of(),
 *         null, 60L, true
 *     ))
 *     .execute()
 *     .await().indefinitely();
 * 
 * // Execute order with fraud detection
 * RunResponse run = client.runs()
 *     .create("order-processing")
 *     .input("orderId", "ORDER-123")
 *     .input("customerId", "CUST-456")
 *     .input("totalAmount", 999.99)
 *     .executeAndStart()
 *     .await().indefinitely();
 * ```
 * 
 * The fraud-detection workflow will execute as a sub-workflow within the
 * order-processing workflow. The parent will wait for completion and receive
 * the fraud risk score, then route to payment or manual review based on the result.
 */

 package tech.kayys.silat.api.subworkflow;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;
import tech.kayys.silat.api.security.TenantContext;
import tech.kayys.silat.core.domain.*;
import tech.kayys.silat.executor.subworkflow.*;

import java.time.Instant;
import java.util.*;

/**
 * ============================================================================
 * SUB-WORKFLOW MANAGEMENT API
 * ============================================================================
 * 
 * REST API for managing sub-workflow relationships and monitoring.
 * 
 * Features:
 * - Query parent-child relationships
 * - Monitor sub-workflow execution
 * - Manage cross-tenant permissions
 * - View sub-workflow hierarchy
 * - Cancel cascading workflows
 * 
 * @author Silat Team
 */

@Path("/api/v1/sub-workflows")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Sub-Workflows", description = "Sub-workflow relationship and monitoring")
public class SubWorkflowResource {
    
    @Inject
    SubWorkflowRelationshipService relationshipService;
    
    @Inject
    SubWorkflowHierarchyService hierarchyService;
    
    @Inject
    TenantContext tenantContext;
    
    // ==================== RELATIONSHIP QUERIES ====================
    
    @GET
    @Path("/parent/{parentRunId}/children")
    @Operation(summary = "Get child workflows for a parent")
    public Uni<List<ChildWorkflowInfo>> getChildren(
            @PathParam("parentRunId") String parentRunId) {
        
        String tenantId = tenantContext.getCurrentTenantId().value();
        
        return relationshipService.getChildWorkflows(
            WorkflowRunId.of(parentRunId),
            TenantId.of(tenantId)
        );
    }
    
    @GET
    @Path("/child/{childRunId}/parent")
    @Operation(summary = "Get parent workflow for a child")
    public Uni<RestResponse<ParentWorkflowInfo>> getParent(
            @PathParam("childRunId") String childRunId) {
        
        String tenantId = tenantContext.getCurrentTenantId().value();
        
        return relationshipService.getParentWorkflow(
                WorkflowRunId.of(childRunId),
                TenantId.of(tenantId))
            .map(parent -> parent != null ?
                RestResponse.ok(parent) :
                RestResponse.notFound());
    }
    
    @GET
    @Path("/{runId}/hierarchy")
    @Operation(summary = "Get complete workflow hierarchy")
    public Uni<WorkflowHierarchy> getHierarchy(
            @PathParam("runId") String runId,
            @QueryParam("maxDepth") @DefaultValue("10") int maxDepth) {
        
        String tenantId = tenantContext.getCurrentTenantId().value();
        
        return hierarchyService.buildHierarchy(
            WorkflowRunId.of(runId),
            TenantId.of(tenantId),
            maxDepth
        );
    }
    
    @GET
    @Path("/{runId}/ancestors")
    @Operation(summary = "Get all ancestor workflows")
    public Uni<List<WorkflowAncestor>> getAncestors(
            @PathParam("runId") String runId) {
        
        String tenantId = tenantContext.getCurrentTenantId().value();
        
        return hierarchyService.getAncestors(
            WorkflowRunId.of(runId),
            TenantId.of(tenantId)
        );
    }
    
    @GET
    @Path("/{runId}/descendants")
    @Operation(summary = "Get all descendant workflows")
    public Uni<List<WorkflowDescendant>> getDescendants(
            @PathParam("runId") String runId) {
        
        String tenantId = tenantContext.getCurrentTenantId().value();
        
        return hierarchyService.getDescendants(
            WorkflowRunId.of(runId),
            TenantId.of(tenantId)
        );
    }
    
    // ==================== MONITORING ====================
    
    @GET
    @Path("/{runId}/status/aggregate")
    @Operation(summary = "Get aggregated status including all sub-workflows")
    public Uni<AggregatedWorkflowStatus> getAggregatedStatus(
            @PathParam("runId") String runId) {
        
        String tenantId = tenantContext.getCurrentTenantId().value();
        
        return hierarchyService.getAggregatedStatus(
            WorkflowRunId.of(runId),
            TenantId.of(tenantId)
        );
    }
    
    @GET
    @Path("/{runId}/metrics/aggregate")
    @Operation(summary = "Get aggregated metrics including all sub-workflows")
    public Uni<AggregatedWorkflowMetrics> getAggregatedMetrics(
            @PathParam("runId") String runId) {
        
        String tenantId = tenantContext.getCurrentTenantId().value();
        
        return hierarchyService.getAggregatedMetrics(
            WorkflowRunId.of(runId),
            TenantId.of(tenantId)
        );
    }
    
    // ==================== CANCELLATION ====================
    
    @POST
    @Path("/{runId}/cancel/cascade")
    @Operation(summary = "Cancel workflow and all descendants")
    public Uni<CascadeCancellationResult> cancelCascade(
            @PathParam("runId") String runId,
            @Valid CancelCascadeRequest request) {
        
        String tenantId = tenantContext.getCurrentTenantId().value();
        
        return relationshipService.cancelCascade(
            WorkflowRunId.of(runId),
            TenantId.of(tenantId),
            request.reason()
        );
    }
    
    // ==================== CROSS-TENANT PERMISSIONS ====================
    
    @GET
    @Path("/permissions/cross-tenant")
    @Operation(summary = "Get cross-tenant sub-workflow permissions")
    public Uni<List<CrossTenantPermission>> getCrossTenantPermissions() {
        
        String tenantId = tenantContext.getCurrentTenantId().value();
        
        return relationshipService.getCrossTenantPermissions(
            TenantId.of(tenantId)
        );
    }
    
    @POST
    @Path("/permissions/cross-tenant")
    @Operation(summary = "Grant cross-tenant sub-workflow permission")
    public Uni<RestResponse<CrossTenantPermission>> grantCrossTenantPermission(
            @Valid GrantCrossTenantPermissionRequest request) {
        
        String sourceTenantId = tenantContext.getCurrentTenantId().value();
        
        return relationshipService.grantCrossTenantPermission(
                TenantId.of(sourceTenantId),
                TenantId.of(request.targetTenantId()),
                request.permissions())
            .map(permission -> RestResponse.status(
                RestResponse.Status.CREATED, permission));
    }
    
    @DELETE
    @Path("/permissions/cross-tenant/{targetTenantId}")
    @Operation(summary = "Revoke cross-tenant sub-workflow permission")
    public Uni<RestResponse<Void>> revokeCrossTenantPermission(
            @PathParam("targetTenantId") String targetTenantId) {
        
        String sourceTenantId = tenantContext.getCurrentTenantId().value();
        
        return relationshipService.revokeCrossTenantPermission(
                TenantId.of(sourceTenantId),
                TenantId.of(targetTenantId))
            .map(v -> RestResponse.ok());
    }
}

// ==================== SERVICE LAYER ====================

/**
 * Sub-workflow relationship service
 */
@jakarta.enterprise.context.ApplicationScoped
class SubWorkflowRelationshipService {
    
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(
        SubWorkflowRelationshipService.class);
    
    @Inject
    SubWorkflowRelationshipRepository repository;
    
    @Inject
    tech.kayys.silat.core.engine.WorkflowRunManager runManager;
    
    /**
     * Get child workflows
     */
    public Uni<List<ChildWorkflowInfo>> getChildWorkflows(
            WorkflowRunId parentRunId,
            TenantId tenantId) {
        
        return repository.findByParent(parentRunId, tenantId)
            .flatMap(relationships -> {
                // Fetch current status of each child
                List<Uni<ChildWorkflowInfo>> childInfos = relationships.stream()
                    .map(rel -> 
                        runManager.getRun(rel.childRunId(), rel.childTenantId())
                            .map(run -> new ChildWorkflowInfo(
                                rel.childRunId().value(),
                                rel.parentNodeId().value(),
                                run.getDefinitionId().value(),
                                run.getStatus().name(),
                                run.getCreatedAt(),
                                run.getStartedAt(),
                                run.getCompletedAt(),
                                rel.childTenantId().value()
                            ))
                    )
                    .toList();
                
                return Uni.join().all(childInfos).andFailFast();
            });
    }
    
    /**
     * Get parent workflow
     */
    public Uni<ParentWorkflowInfo> getParentWorkflow(
            WorkflowRunId childRunId,
            TenantId tenantId) {
        
        return repository.findByChild(childRunId, tenantId)
            .flatMap(rel -> {
                if (rel == null) {
                    return Uni.createFrom().nullItem();
                }
                
                return runManager.getRun(rel.parentRunId(), rel.parentTenantId())
                    .map(run -> new ParentWorkflowInfo(
                        rel.parentRunId().value(),
                        rel.parentNodeId().value(),
                        run.getDefinitionId().value(),
                        run.getStatus().name(),
                        rel.parentTenantId().value()
                    ));
            });
    }
    
    /**
     * Cancel workflow cascade
     */
    public Uni<CascadeCancellationResult> cancelCascade(
            WorkflowRunId rootRunId,
            TenantId tenantId,
            String reason) {
        
        LOG.info("Cancelling workflow cascade: root={}, reason={}", 
            rootRunId.value(), reason);
        
        return collectDescendants(rootRunId, tenantId)
            .flatMap(descendants -> {
                // Cancel in reverse order (children first)
                List<WorkflowRunId> allRuns = new ArrayList<>();
                descendants.forEach(desc -> allRuns.add(
                    WorkflowRunId.of(desc.runId())));
                allRuns.add(rootRunId);
                
                Collections.reverse(allRuns);
                
                List<Uni<Void>> cancellations = allRuns.stream()
                    .map(runId -> 
                        runManager.cancelRun(runId, tenantId, reason)
                            .onFailure().recoverWithNull()
                    )
                    .toList();
                
                return Uni.join().all(cancellations).andFailFast()
                    .map(v -> new CascadeCancellationResult(
                        true,
                        allRuns.size(),
                        reason,
                        Instant.now()
                    ));
            });
    }
    
    private Uni<List<WorkflowDescendant>> collectDescendants(
            WorkflowRunId runId,
            TenantId tenantId) {
        
        return repository.findByParent(runId, tenantId)
            .flatMap(children -> {
                if (children.isEmpty()) {
                    return Uni.createFrom().item(List.of());
                }
                
                List<Uni<List<WorkflowDescendant>>> descendants = children.stream()
                    .map(child -> {
                        Uni<List<WorkflowDescendant>> childDescendants = 
                            collectDescendants(child.childRunId(), child.childTenantId());
                        
                        return runManager.getRun(child.childRunId(), child.childTenantId())
                            .flatMap(run -> 
                                childDescendants.map(list -> {
                                    List<WorkflowDescendant> result = new ArrayList<>();
                                    result.add(new WorkflowDescendant(
                                        child.childRunId().value(),
                                        run.getDefinitionId().value(),
                                        run.getStatus().name(),
                                        1
                                    ));
                                    list.forEach(d -> result.add(
                                        new WorkflowDescendant(
                                            d.runId(),
                                            d.workflowDefinitionId(),
                                            d.status(),
                                            d.depth() + 1
                                        )));
                                    return result;
                                })
                            );
                    })
                    .toList();
                
                return Uni.join().all(descendants).andFailFast()
                    .map(lists -> lists.stream()
                        .flatMap(List::stream)
                        .toList());
            });
    }
    
    /**
     * Get cross-tenant permissions
     */
    public Uni<List<CrossTenantPermission>> getCrossTenantPermissions(
            TenantId tenantId) {
        
        return repository.findCrossTenantPermissions(tenantId);
    }
    
    /**
     * Grant cross-tenant permission
     */
    public Uni<CrossTenantPermission> grantCrossTenantPermission(
            TenantId sourceTenantId,
            TenantId targetTenantId,
            List<String> permissions) {
        
        CrossTenantPermission permission = new CrossTenantPermission(
            UUID.randomUUID().toString(),
            sourceTenantId.value(),
            targetTenantId.value(),
            permissions,
            Instant.now(),
            null // No expiration
        );
        
        return repository.saveCrossTenantPermission(permission);
    }
    
    /**
     * Revoke cross-tenant permission
     */
    public Uni<Void> revokeCrossTenantPermission(
            TenantId sourceTenantId,
            TenantId targetTenantId) {
        
        return repository.deleteCrossTenantPermission(sourceTenantId, targetTenantId);
    }
}

/**
 * Sub-workflow hierarchy service
 */
@jakarta.enterprise.context.ApplicationScoped
class SubWorkflowHierarchyService {
    
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(
        SubWorkflowHierarchyService.class);
    
    @Inject
    SubWorkflowRelationshipRepository repository;
    
    @Inject
    tech.kayys.silat.core.engine.WorkflowRunManager runManager;
    
    /**
     * Build complete workflow hierarchy
     */
    public Uni<WorkflowHierarchy> buildHierarchy(
            WorkflowRunId runId,
            TenantId tenantId,
            int maxDepth) {
        
        return runManager.getRun(runId, tenantId)
            .flatMap(root -> {
                WorkflowHierarchyNode rootNode = new WorkflowHierarchyNode(
                    runId.value(),
                    root.getDefinitionId().value(),
                    root.getStatus().name(),
                    0,
                    new ArrayList<>()
                );
                
                return buildChildHierarchy(rootNode, runId, tenantId, 1, maxDepth)
                    .map(node -> new WorkflowHierarchy(
                        node,
                        calculateTotalNodes(node),
                        calculateMaxDepth(node)
                    ));
            });
    }
    
    private Uni<WorkflowHierarchyNode> buildChildHierarchy(
            WorkflowHierarchyNode node,
            WorkflowRunId runId,
            TenantId tenantId,
            int currentDepth,
            int maxDepth) {
        
        if (currentDepth > maxDepth) {
            return Uni.createFrom().item(node);
        }
        
        return repository.findByParent(runId, tenantId)
            .flatMap(children -> {
                if (children.isEmpty()) {
                    return Uni.createFrom().item(node);
                }
                
                List<Uni<WorkflowHierarchyNode>> childNodes = children.stream()
                    .map(child -> 
                        runManager.getRun(child.childRunId(), child.childTenantId())
                            .flatMap(run -> {
                                WorkflowHierarchyNode childNode = 
                                    new WorkflowHierarchyNode(
                                        child.childRunId().value(),
                                        run.getDefinitionId().value(),
                                        run.getStatus().name(),
                                        currentDepth,
                                        new ArrayList<>()
                                    );
                                
                                return buildChildHierarchy(
                                    childNode,
                                    child.childRunId(),
                                    child.childTenantId(),
                                    currentDepth + 1,
                                    maxDepth
                                );
                            })
                    )
                    .toList();
                
                return Uni.join().all(childNodes).andFailFast()
                    .map(nodes -> {
                        node.children().addAll(nodes);
                        return node;
                    });
            });
    }
    
    /**
     * Get ancestors (parent, grandparent, etc.)
     */
    public Uni<List<WorkflowAncestor>> getAncestors(
            WorkflowRunId runId,
            TenantId tenantId) {
        
        return collectAncestors(runId, tenantId, new ArrayList<>(), 1);
    }
    
    private Uni<List<WorkflowAncestor>> collectAncestors(
            WorkflowRunId runId,
            TenantId tenantId,
            List<WorkflowAncestor> ancestors,
            int level) {
        
        return repository.findByChild(runId, tenantId)
            .flatMap(rel -> {
                if (rel == null) {
                    return Uni.createFrom().item(ancestors);
                }
                
                return runManager.getRun(rel.parentRunId(), rel.parentTenantId())
                    .flatMap(run -> {
                        ancestors.add(new WorkflowAncestor(
                            rel.parentRunId().value(),
                            run.getDefinitionId().value(),
                            run.getStatus().name(),
                            level
                        ));
                        
                        return collectAncestors(
                            rel.parentRunId(),
                            rel.parentTenantId(),
                            ancestors,
                            level + 1
                        );
                    });
            });
    }
    
    /**
     * Get descendants (children, grandchildren, etc.)
     */
    public Uni<List<WorkflowDescendant>> getDescendants(
            WorkflowRunId runId,
            TenantId tenantId) {
        
        return collectDescendants(runId, tenantId, new ArrayList<>(), 1);
    }
    
    private Uni<List<WorkflowDescendant>> collectDescendants(
            WorkflowRunId runId,
            TenantId tenantId,
            List<WorkflowDescendant> descendants,
            int level) {
        
        return repository.findByParent(runId, tenantId)
            .flatMap(children -> {
                if (children.isEmpty()) {
                    return Uni.createFrom().item(descendants);
                }
                
                List<Uni<List<WorkflowDescendant>>> childDescendants = 
                    children.stream()
                        .map(child -> 
                            runManager.getRun(child.childRunId(), child.childTenantId())
                                .flatMap(run -> {
                                    descendants.add(new WorkflowDescendant(
                                        child.childRunId().value(),
                                        run.getDefinitionId().value(),
                                        run.getStatus().name(),
                                        level
                                    ));
                                    
                                    return collectDescendants(
                                        child.childRunId(),
                                        child.childTenantId(),
                                        descendants,
                                        level + 1
                                    );
                                })
                        )
                        .toList();
                
                return Uni.join().all(childDescendants).andFailFast()
                    .map(v -> descendants);
            });
    }
    
    /**
     * Get aggregated status
     */
    public Uni<AggregatedWorkflowStatus> getAggregatedStatus(
            WorkflowRunId runId,
            TenantId tenantId) {
        
        return getDescendants(runId, tenantId)
            .flatMap(descendants -> 
                runManager.getRun(runId, tenantId)
                    .map(root -> {
                        Map<String, Integer> statusCounts = new HashMap<>();
                        statusCounts.put(root.getStatus().name(), 1);
                        
                        descendants.forEach(desc -> {
                            statusCounts.merge(desc.status(), 1, Integer::sum);
                        });
                        
                        return new AggregatedWorkflowStatus(
                            runId.value(),
                            root.getStatus().name(),
                            descendants.size() + 1,
                            statusCounts
                        );
                    })
            );
    }
    
    /**
     * Get aggregated metrics
     */
    public Uni<AggregatedWorkflowMetrics> getAggregatedMetrics(
            WorkflowRunId runId,
            TenantId tenantId) {
        
        return getDescendants(runId, tenantId)
            .flatMap(descendants -> {
                List<WorkflowRunId> allRuns = new ArrayList<>();
                allRuns.add(runId);
                descendants.forEach(d -> allRuns.add(WorkflowRunId.of(d.runId())));
                
                List<Uni<WorkflowRun>> runFetches = allRuns.stream()
                    .map(id -> runManager.getRun(id, tenantId))
                    .toList();
                
                return Uni.join().all(runFetches).andFailFast()
                    .map(runs -> {
                        long totalDuration = 0;
                        int completed = 0;
                        
                        for (WorkflowRun run : runs) {
                            if (run.getCompletedAt() != null && 
                                run.getStartedAt() != null) {
                                totalDuration += java.time.Duration.between(
                                    run.getStartedAt(),
                                    run.getCompletedAt()
                                ).toMillis();
                                completed++;
                            }
                        }
                        
                        long avgDuration = completed > 0 ? 
                            totalDuration / completed : 0;
                        
                        return new AggregatedWorkflowMetrics(
                            runId.value(),
                            runs.size(),
                            completed,
                            avgDuration
                        );
                    });
            });
    }
    
    private int calculateTotalNodes(WorkflowHierarchyNode node) {
        int count = 1;
        for (WorkflowHierarchyNode child : node.children()) {
            count += calculateTotalNodes(child);
        }
        return count;
    }
    
    private int calculateMaxDepth(WorkflowHierarchyNode node) {
        if (node.children().isEmpty()) {
            return node.depth();
        }
        return node.children().stream()
            .mapToInt(this::calculateMaxDepth)
            .max()
            .orElse(node.depth());
    }
}

// ==================== DTOs ====================

record ChildWorkflowInfo(
    String runId,
    String parentNodeId,
    String workflowDefinitionId,
    String status,
    Instant createdAt,
    Instant startedAt,
    Instant completedAt,
    String tenantId
) {}

record ParentWorkflowInfo(
    String runId,
    String parentNodeId,
    String workflowDefinitionId,
    String status,
    String tenantId
) {}

record WorkflowHierarchy(
    WorkflowHierarchyNode root,
    int totalNodes,
    int maxDepth
) {}

record WorkflowHierarchyNode(
    String runId,
    String workflowDefinitionId,
    String status,
    int depth,
    List<WorkflowHierarchyNode> children
) {}

record WorkflowAncestor(
    String runId,
    String workflowDefinitionId,
    String status,
    int level
) {}

record WorkflowDescendant(
    String runId,
    String workflowDefinitionId,
    String status,
    int depth
) {}

record AggregatedWorkflowStatus(
    String rootRunId,
    String rootStatus,
    int totalWorkflows,
    Map<String, Integer> statusCounts
) {}

record AggregatedWorkflowMetrics(
    String rootRunId,
    int totalWorkflows,
    int completedWorkflows,
    long averageDurationMs
) {}

record CascadeCancellationResult(
    boolean success,
    int workflowsCancelled,
    String reason,
    Instant cancelledAt
) {}

record CrossTenantPermission(
    String permissionId,
    String sourceTenantId,
    String targetTenantId,
    List<String> permissions,
    Instant grantedAt,
    Instant expiresAt
) {}

record CancelCascadeRequest(
    @NotNull String reason
) {}

record GrantCrossTenantPermissionRequest(
    @NotNull String targetTenantId,
    @NotNull List<String> permissions
) {}


package tech.kayys.silat.persistence.subworkflow;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import tech.kayys.silat.api.subworkflow.CrossTenantPermission;
import tech.kayys.silat.core.domain.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ============================================================================
 * SUB-WORKFLOW RELATIONSHIP REPOSITORY
 * ============================================================================
 * 
 * Manages persistence of parent-child workflow relationships.
 * 
 * Features:
 * - Bi-directional relationship tracking
 * - Cross-tenant relationship support
 * - Efficient hierarchical queries
 * - Permission management
 * - Audit trail
 * 
 * Database Schema:
 * - sub_workflow_relationships: Parent-child mappings
 * - cross_tenant_permissions: Cross-tenant access control
 * - sub_workflow_audit: Audit log for relationship changes
 * 
 * @author Silat Team
 */

// ==================== ENTITY CLASSES ====================

/**
 * Sub-workflow relationship entity
 * Tracks parent-child workflow relationships
 */
@Entity
@Table(
    name = "sub_workflow_relationships",
    indexes = {
        @Index(name = "idx_parent_run", columnList = "parent_run_id"),
        @Index(name = "idx_child_run", columnList = "child_run_id"),
        @Index(name = "idx_parent_tenant", columnList = "parent_tenant_id"),
        @Index(name = "idx_child_tenant", columnList = "child_tenant_id"),
        @Index(name = "idx_created_at", columnList = "created_at")
    }
)
public class SubWorkflowRelationshipEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "relationship_id")
    private UUID relationshipId;
    
    /**
     * Parent workflow information
     */
    @Column(name = "parent_run_id", nullable = false)
    private String parentRunId;
    
    @Column(name = "parent_node_id", nullable = false)
    private String parentNodeId;
    
    @Column(name = "parent_tenant_id", nullable = false)
    private String parentTenantId;
    
    @Column(name = "parent_definition_id")
    private String parentDefinitionId;
    
    /**
     * Child workflow information
     */
    @Column(name = "child_run_id", nullable = false, unique = true)
    private String childRunId;
    
    @Column(name = "child_tenant_id", nullable = false)
    private String childTenantId;
    
    @Column(name = "child_definition_id")
    private String childDefinitionId;
    
    /**
     * Relationship metadata
     */
    @Column(name = "nesting_depth")
    private Integer nestingDepth;
    
    @Column(name = "is_cross_tenant")
    private Boolean isCrossTenant;
    
    @Column(name = "execution_mode")
    @Enumerated(EnumType.STRING)
    private SubWorkflowExecutionMode executionMode;
    
    /**
     * Audit fields
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private RelationshipStatus status;
    
    // Getters and setters
    public UUID getRelationshipId() { return relationshipId; }
    public void setRelationshipId(UUID relationshipId) { 
        this.relationshipId = relationshipId; 
    }
    
    public String getParentRunId() { return parentRunId; }
    public void setParentRunId(String parentRunId) { 
        this.parentRunId = parentRunId; 
    }
    
    public String getParentNodeId() { return parentNodeId; }
    public void setParentNodeId(String parentNodeId) { 
        this.parentNodeId = parentNodeId; 
    }
    
    public String getParentTenantId() { return parentTenantId; }
    public void setParentTenantId(String parentTenantId) { 
        this.parentTenantId = parentTenantId; 
    }
    
    public String getChildRunId() { return childRunId; }
    public void setChildRunId(String childRunId) { 
        this.childRunId = childRunId; 
    }
    
    public String getChildTenantId() { return childTenantId; }
    public void setChildTenantId(String childTenantId) { 
        this.childTenantId = childTenantId; 
    }
    
    public Integer getNestingDepth() { return nestingDepth; }
    public void setNestingDepth(Integer nestingDepth) { 
        this.nestingDepth = nestingDepth; 
    }
    
    public Boolean getIsCrossTenant() { return isCrossTenant; }
    public void setIsCrossTenant(Boolean isCrossTenant) { 
        this.isCrossTenant = isCrossTenant; 
    }
    
    public SubWorkflowExecutionMode getExecutionMode() { return executionMode; }
    public void setExecutionMode(SubWorkflowExecutionMode executionMode) { 
        this.executionMode = executionMode; 
    }
    
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { 
        this.completedAt = completedAt; 
    }
    
    public RelationshipStatus getStatus() { return status; }
    public void setStatus(RelationshipStatus status) { 
        this.status = status; 
    }
}

/**
 * Cross-tenant permission entity
 */
@Entity
@Table(
    name = "cross_tenant_permissions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_tenant_pair",
            columnNames = {"source_tenant_id", "target_tenant_id"}
        )
    }
)
public class CrossTenantPermissionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "permission_id")
    private UUID permissionId;
    
    @Column(name = "source_tenant_id", nullable = false)
    private String sourceTenantId;
    
    @Column(name = "target_tenant_id", nullable = false)
    private String targetTenantId;
    
    @Column(name = "permissions", columnDefinition = "text[]")
    private String[] permissions;
    
    @CreationTimestamp
    @Column(name = "granted_at", updatable = false)
    private Instant grantedAt;
    
    @Column(name = "granted_by")
    private String grantedBy;
    
    @Column(name = "expires_at")
    private Instant expiresAt;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    // Getters and setters
    public UUID getPermissionId() { return permissionId; }
    public String getSourceTenantId() { return sourceTenantId; }
    public void setSourceTenantId(String sourceTenantId) { 
        this.sourceTenantId = sourceTenantId; 
    }
    
    public String getTargetTenantId() { return targetTenantId; }
    public void setTargetTenantId(String targetTenantId) { 
        this.targetTenantId = targetTenantId; 
    }
    
    public String[] getPermissions() { return permissions; }
    public void setPermissions(String[] permissions) { 
        this.permissions = permissions; 
    }
    
    public Instant getGrantedAt() { return grantedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { 
        this.expiresAt = expiresAt; 
    }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { 
        this.isActive = isActive; 
    }
}

/**
 * Sub-workflow audit entity
 */
@Entity
@Table(
    name = "sub_workflow_audit",
    indexes = {
        @Index(name = "idx_audit_run", columnList = "run_id"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp")
    }
)
public class SubWorkflowAuditEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "audit_id")
    private UUID auditId;
    
    @Column(name = "run_id", nullable = false)
    private String runId;
    
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    
    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditEventType eventType;
    
    @Column(name = "event_data", columnDefinition = "jsonb")
    private String eventData;
    
    @CreationTimestamp
    @Column(name = "timestamp", updatable = false)
    private Instant timestamp;
    
    @Column(name = "user_id")
    private String userId;
    
    // Getters and setters
    public UUID getAuditId() { return auditId; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public AuditEventType getEventType() { return eventType; }
    public void setEventType(AuditEventType eventType) { 
        this.eventType = eventType; 
    }
    
    public String getEventData() { return eventData; }
    public void setEventData(String eventData) { 
        this.eventData = eventData; 
    }
    
    public Instant getTimestamp() { return timestamp; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}

// ==================== ENUMS ====================

enum SubWorkflowExecutionMode {
    SYNCHRONOUS,    // Wait for completion
    ASYNCHRONOUS,   // Fire and forget
    DETACHED        // Detached execution
}

enum RelationshipStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED,
    FAILED
}

enum AuditEventType {
    RELATIONSHIP_CREATED,
    RELATIONSHIP_COMPLETED,
    RELATIONSHIP_CANCELLED,
    PERMISSION_GRANTED,
    PERMISSION_REVOKED,
    CROSS_TENANT_ACCESS
}

// ==================== REPOSITORY ====================

/**
 * Sub-workflow relationship repository
 */
@ApplicationScoped
public class SubWorkflowRelationshipRepository 
        implements PanacheRepositoryBase<SubWorkflowRelationshipEntity, UUID> {
    
    private static final org.slf4j.Logger LOG = 
        org.slf4j.LoggerFactory.getLogger(SubWorkflowRelationshipRepository.class);
    
    /**
     * Create relationship when child workflow is launched
     */
    public Uni<SubWorkflowRelationship> createRelationship(
            WorkflowRunId parentRunId,
            NodeId parentNodeId,
            TenantId parentTenantId,
            WorkflowRunId childRunId,
            TenantId childTenantId,
            int nestingDepth,
            SubWorkflowExecutionMode executionMode) {
        
        return Panache.withTransaction(() -> {
            SubWorkflowRelationshipEntity entity = new SubWorkflowRelationshipEntity();
            entity.setParentRunId(parentRunId.value());
            entity.setParentNodeId(parentNodeId.value());
            entity.setParentTenantId(parentTenantId.value());
            entity.setChildRunId(childRunId.value());
            entity.setChildTenantId(childTenantId.value());
            entity.setNestingDepth(nestingDepth);
            entity.setIsCrossTenant(!parentTenantId.equals(childTenantId));
            entity.setExecutionMode(executionMode);
            entity.setStatus(RelationshipStatus.ACTIVE);
            
            return persist(entity)
                .map(this::toRelationship)
                .invoke(rel -> LOG.info(
                    "Created sub-workflow relationship: parent={}, child={}",
                    parentRunId.value(), childRunId.value()));
        });
    }
    
    /**
     * Find all child workflows for a parent
     */
    public Uni<List<SubWorkflowRelationship>> findByParent(
            WorkflowRunId parentRunId,
            TenantId tenantId) {
        
        return find(
            "parentRunId = ?1 and parentTenantId = ?2",
            parentRunId.value(),
            tenantId.value()
        ).list()
        .map(entities -> entities.stream()
            .map(this::toRelationship)
            .toList());
    }
    
    /**
     * Find parent workflow for a child
     */
    public Uni<SubWorkflowRelationship> findByChild(
            WorkflowRunId childRunId,
            TenantId tenantId) {
        
        return find(
            "childRunId = ?1 and childTenantId = ?2",
            childRunId.value(),
            tenantId.value()
        ).firstResult()
        .map(entity -> entity != null ? toRelationship(entity) : null);
    }
    
    /**
     * Update relationship status
     */
    public Uni<Void> updateStatus(
            WorkflowRunId childRunId,
            RelationshipStatus status) {
        
        return Panache.withTransaction(() ->
            update("status = ?1, completedAt = ?2 where childRunId = ?3",
                status,
                status == RelationshipStatus.COMPLETED ? Instant.now() : null,
                childRunId.value()
            ).replaceWithVoid()
        );
    }
    
    /**
     * Find cross-tenant permissions
     */
    public Uni<List<CrossTenantPermission>> findCrossTenantPermissions(
            TenantId tenantId) {
        
        return getEntityManager()
            .createQuery(
                "SELECT p FROM CrossTenantPermissionEntity p " +
                "WHERE (p.sourceTenantId = :tenantId OR p.targetTenantId = :tenantId) " +
                "AND p.isActive = true " +
                "AND (p.expiresAt IS NULL OR p.expiresAt > :now)",
                CrossTenantPermissionEntity.class
            )
            .setParameter("tenantId", tenantId.value())
            .setParameter("now", Instant.now())
            .getResultList()
            .map(entities -> entities.stream()
                .map(this::toPermission)
                .toList());
    }
    
    /**
     * Save cross-tenant permission
     */
    public Uni<CrossTenantPermission> saveCrossTenantPermission(
            CrossTenantPermission permission) {
        
        return Panache.withTransaction(() -> {
            CrossTenantPermissionEntity entity = new CrossTenantPermissionEntity();
            entity.setSourceTenantId(permission.sourceTenantId());
            entity.setTargetTenantId(permission.targetTenantId());
            entity.setPermissions(permission.permissions().toArray(new String[0]));
            entity.setExpiresAt(permission.expiresAt());
            
            return getEntityManager().persist(entity)
                .map(v -> toPermission(entity));
        });
    }
    
    /**
     * Delete cross-tenant permission
     */
    public Uni<Void> deleteCrossTenantPermission(
            TenantId sourceTenantId,
            TenantId targetTenantId) {
        
        return Panache.withTransaction(() ->
            getEntityManager()
                .createQuery(
                    "UPDATE CrossTenantPermissionEntity " +
                    "SET isActive = false " +
                    "WHERE sourceTenantId = :source AND targetTenantId = :target"
                )
                .setParameter("source", sourceTenantId.value())
                .setParameter("target", targetTenantId.value())
                .executeUpdate()
                .replaceWithVoid()
        );
    }
    
    /**
     * Audit event
     */
    public Uni<Void> auditEvent(
            WorkflowRunId runId,
            TenantId tenantId,
            AuditEventType eventType,
            String eventData) {
        
        return Panache.withTransaction(() -> {
            SubWorkflowAuditEntity audit = new SubWorkflowAuditEntity();
            audit.setRunId(runId.value());
            audit.setTenantId(tenantId.value());
            audit.setEventType(eventType);
            audit.setEventData(eventData);
            
            return getEntityManager().persist(audit).replaceWithVoid();
        });
    }
    
    // ==================== MAPPING METHODS ====================
    
    private SubWorkflowRelationship toRelationship(SubWorkflowRelationshipEntity entity) {
        return new SubWorkflowRelationship(
            WorkflowRunId.of(entity.getParentRunId()),
            NodeId.of(entity.getParentNodeId()),
            TenantId.of(entity.getParentTenantId()),
            WorkflowRunId.of(entity.getChildRunId()),
            TenantId.of(entity.getChildTenantId()),
            entity.getNestingDepth(),
            entity.getIsCrossTenant(),
            entity.getCreatedAt()
        );
    }
    
    private CrossTenantPermission toPermission(CrossTenantPermissionEntity entity) {
        return new CrossTenantPermission(
            entity.getPermissionId().toString(),
            entity.getSourceTenantId(),
            entity.getTargetTenantId(),
            List.of(entity.getPermissions()),
            entity.getGrantedAt(),
            entity.getExpiresAt()
        );
    }
    
    private Uni<EntityManager> getEntityManager() {
        return Panache.currentSession()
            .map(session -> session.unwrap(EntityManager.class));
    }
}

/**
 * Sub-workflow relationship domain object
 */
record SubWorkflowRelationship(
    WorkflowRunId parentRunId,
    NodeId parentNodeId,
    TenantId parentTenantId,
    WorkflowRunId childRunId,
    TenantId childTenantId,
    int nestingDepth,
    boolean isCrossTenant,
    Instant createdAt
) {}

// ==================== DATABASE MIGRATION SQL ====================

/**
 * Liquibase/Flyway migration script:
 * 
 * -- V1__Create_SubWorkflow_Tables.sql
 * 
 * CREATE TABLE sub_workflow_relationships (
 *     relationship_id UUID PRIMARY KEY,
 *     parent_run_id VARCHAR(255) NOT NULL,
 *     parent_node_id VARCHAR(255) NOT NULL,
 *     parent_tenant_id VARCHAR(255) NOT NULL,
 *     parent_definition_id VARCHAR(255),
 *     child_run_id VARCHAR(255) NOT NULL UNIQUE,
 *     child_tenant_id VARCHAR(255) NOT NULL,
 *     child_definition_id VARCHAR(255),
 *     nesting_depth INTEGER,
 *     is_cross_tenant BOOLEAN DEFAULT FALSE,
 *     execution_mode VARCHAR(50),
 *     status VARCHAR(50) NOT NULL,
 *     created_at TIMESTAMP NOT NULL DEFAULT NOW(),
 *     completed_at TIMESTAMP,
 *     CONSTRAINT fk_parent_workflow FOREIGN KEY (parent_run_id) 
 *         REFERENCES workflow_runs(run_id) ON DELETE CASCADE,
 *     CONSTRAINT fk_child_workflow FOREIGN KEY (child_run_id) 
 *         REFERENCES workflow_runs(run_id) ON DELETE CASCADE
 * );
 * 
 * CREATE INDEX idx_parent_run ON sub_workflow_relationships(parent_run_id);
 * CREATE INDEX idx_child_run ON sub_workflow_relationships(child_run_id);
 * CREATE INDEX idx_parent_tenant ON sub_workflow_relationships(parent_tenant_id);
 * CREATE INDEX idx_child_tenant ON sub_workflow_relationships(child_tenant_id);
 * CREATE INDEX idx_created_at ON sub_workflow_relationships(created_at);
 * 
 * CREATE TABLE cross_tenant_permissions (
 *     permission_id UUID PRIMARY KEY,
 *     source_tenant_id VARCHAR(255) NOT NULL,
 *     target_tenant_id VARCHAR(255) NOT NULL,
 *     permissions TEXT[],
 *     granted_at TIMESTAMP NOT NULL DEFAULT NOW(),
 *     granted_by VARCHAR(255),
 *     expires_at TIMESTAMP,
 *     is_active BOOLEAN DEFAULT TRUE,
 *     CONSTRAINT uk_tenant_pair UNIQUE (source_tenant_id, target_tenant_id)
 * );
 * 
 * CREATE TABLE sub_workflow_audit (
 *     audit_id UUID PRIMARY KEY,
 *     run_id VARCHAR(255) NOT NULL,
 *     tenant_id VARCHAR(255) NOT NULL,
 *     event_type VARCHAR(100) NOT NULL,
 *     event_data JSONB,
 *     timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
 *     user_id VARCHAR(255)
 * );
 * 
 * CREATE INDEX idx_audit_run ON sub_workflow_audit(run_id);
 * CREATE INDEX idx_audit_timestamp ON sub_workflow_audit(timestamp);
 */

 package tech.kayys.silat.executor.subworkflow;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;
import tech.kayys.silat.client.SilatClient;
import tech.kayys.silat.core.domain.*;
import tech.kayys.silat.api.dto.*;

import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ============================================================================
 * SUB-WORKFLOW EXECUTOR TESTS
 * ============================================================================
 * 
 * Comprehensive tests for sub-workflow functionality.
 * 
 * Test Coverage:
 * - Basic sub-workflow execution
 * - Input/output mapping
 * - Error propagation strategies
 * - Timeout handling
 * - Cancellation cascade
 * - Cross-tenant invocation
 * - Nested sub-workflows (multiple levels)
 * - Parallel sub-workflows
 * - Fire-and-forget mode
 * 
 * @author Silat Team
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SubWorkflowExecutorTest {
    
    @Inject
    SubWorkflowExecutor subWorkflowExecutor;
    
    @Inject
    SubWorkflowRelationshipService relationshipService;
    
    private SilatClient client;
    
    @BeforeEach
    void setup() {
        client = SilatClient.builder()
            .restEndpoint("http://localhost:8080")
            .tenantId("test-tenant")
            .apiKey("test-key")
            .build();
    }
    
    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
    }
    
    // ==================== BASIC TESTS ====================
    
    @Test
    @Order(1)
    @DisplayName("Test basic sub-workflow execution")
    void testBasicSubWorkflowExecution() {
        // Create child workflow
        WorkflowDefinitionResponse childWorkflow = createSimpleChildWorkflow();
        
        // Create parent workflow with sub-workflow node
        WorkflowDefinitionResponse parentWorkflow = client.workflows()
            .create("parent-workflow")
            .version("1.0.0")
            .addNode(createSubWorkflowNode(
                "call-child",
                childWorkflow.definitionId(),
                Map.of("input1", "parentInput1"),
                Map.of("parentOutput1", "output1")
            ))
            .addInput("parentInput1", stringInput("Parent input", true))
            .addOutput("parentOutput1", stringOutput("Parent output"))
            .execute()
            .await().atMost(Duration.ofSeconds(10));
        
        // Execute parent workflow
        RunResponse run = client.runs()
            .create(parentWorkflow.definitionId())
            .input("parentInput1", "test-value")
            .executeAndStart()
            .await().atMost(Duration.ofSeconds(10));
        
        // Wait for completion
        RunResponse completed = waitForCompletion(run.runId());
        
        assertEquals("COMPLETED", completed.status());
        assertNotNull(completed.variables().get("parentOutput1"));
        
        // Verify relationship was created
        List<ChildWorkflowInfo> children = relationshipService
            .getChildWorkflows(
                WorkflowRunId.of(run.runId()),
                TenantId.of("test-tenant"))
            .await().atMost(Duration.ofSeconds(5));
        
        assertEquals(1, children.size());
    }
    
    @Test
    @Order(2)
    @DisplayName("Test input/output mapping")
    void testInputOutputMapping() {
        WorkflowDefinitionResponse childWorkflow = createMappingTestWorkflow();
        
        WorkflowDefinitionResponse parentWorkflow = client.workflows()
            .create("mapping-parent")
            .version("1.0.0")
            .addNode(createSubWorkflowNode(
                "mapped-child",
                childWorkflow.definitionId(),
                Map.of(
                    "childField1", "parentField1",
                    "childField2", "parentField2"
                ),
                Map.of(
                    "parentResult1", "childResult1",
                    "parentResult2", "childResult2"
                )
            ))
            .addInput("parentField1", stringInput("Field 1", true))
            .addInput("parentField2", stringInput("Field 2", true))
            .addOutput("parentResult1", stringOutput("Result 1"))
            .addOutput("parentResult2", stringOutput("Result 2"))
            .execute()
            .await().atMost(Duration.ofSeconds(10));
        
        RunResponse run = client.runs()
            .create(parentWorkflow.definitionId())
            .input("parentField1", "value1")
            .input("parentField2", "value2")
            .executeAndStart()
            .await().atMost(Duration.ofSeconds(10));
        
        RunResponse completed = waitForCompletion(run.runId());
        
        assertEquals("COMPLETED", completed.status());
        assertEquals("transformed-value1", completed.variables().get("parentResult1"));
        assertEquals("transformed-value2", completed.variables().get("parentResult2"));
    }
    
    @Test
    @Order(3)
    @DisplayName("Test error propagation")
    void testErrorPropagation() {
        WorkflowDefinitionResponse failingChild = createFailingWorkflow();
        
        WorkflowDefinitionResponse parentWorkflow = client.workflows()
            .create("error-parent")
            .version("1.0.0")
            .addNode(createSubWorkflowNode(
                "failing-child",
                failingChild.definitionId(),
                Map.of(),
                Map.of(),
                "PROPAGATE" // Error strategy
            ))
            .execute()
            .await().atMost(Duration.ofSeconds(10));
        
        RunResponse run = client.runs()
            .create(parentWorkflow.definitionId())
            .executeAndStart()
            .await().atMost(Duration.ofSeconds(10));
        
        RunResponse completed = waitForCompletion(run.runId());
        
        assertEquals("FAILED", completed.status());
        assertTrue(completed.nodeExecutions().get("failing-child")
            .error().message().contains("Sub-workflow execution failed"));
    }
    
    @Test
    @Order(4)
    @DisplayName("Test error ignore strategy")
    void testErrorIgnoreStrategy() {
        WorkflowDefinitionResponse failingChild = createFailingWorkflow();
        
        WorkflowDefinitionResponse parentWorkflow = client.workflows()
            .create("ignore-error-parent")
            .version("1.0.0")
            .addNode(createSubWorkflowNode(
                "failing-child-ignored",
                failingChild.definitionId(),
                Map.of(),
                Map.of(),
                "IGNORE" // Error strategy
            ))
            .execute()
            .await().atMost(Duration.ofSeconds(10));
        
        RunResponse run = client.runs()
            .create(parentWorkflow.definitionId())
            .executeAndStart()
            .await().atMost(Duration.ofSeconds(10));
        
        RunResponse completed = waitForCompletion(run.runId());
        
        // Parent should succeed despite child failure
        assertEquals("COMPLETED", completed.status());
        assertTrue((Boolean) completed.nodeExecutions()
            .get("failing-child-ignored")
            .output().get("errorIgnored"));
    }
    
    @Test
    @Order(5)
    @DisplayName("Test sub-workflow timeout")
    void testSubWorkflowTimeout() {
        WorkflowDefinitionResponse slowChild = createSlowWorkflow(Duration.ofSeconds(60));
        
        WorkflowDefinitionResponse parentWorkflow = client.workflows()
            .create("timeout-parent")
            .version("1.0.0")
            .addNode(createSubWorkflowNodeWithTimeout(
                "slow-child",
                slowChild.definitionId(),
                5 // 5 second timeout
            ))
            .execute()
            .await().atMost(Duration.ofSeconds(10));
        
        RunResponse run = client.runs()
            .create(parentWorkflow.definitionId())
            .executeAndStart()
            .await().atMost(Duration.ofSeconds(10));
        
        RunResponse completed = waitForCompletion(run.runId());
        
        assertEquals("FAILED", completed.status());
        assertTrue(completed.nodeExecutions().get("slow-child")
            .error().code().equals("SUB_WORKFLOW_TIMEOUT"));
    }
    
    // ==================== ADVANCED TESTS ====================
    
    @Test
    @Order(6)
    @DisplayName("Test nested sub-workflows (3 levels)")
    void testNestedSubWorkflows() {
        // Level 3 (innermost)
        WorkflowDefinitionResponse level3 = createSimpleChildWorkflow("level-3");
        
        // Level 2 (calls level 3)
        WorkflowDefinitionResponse level2 = client.workflows()
            .create("level-2")
            .version("1.0.0")
            .addNode(createSubWorkflowNode("call-level-3", level3.definitionId()))
            .execute()
            .await().atMost(Duration.ofSeconds(10));
        
        // Level 1 (calls level 2)
        WorkflowDefinitionResponse level1 = client.workflows()
            .create("level-1")
            .version("1.0.0")
            .addNode(createSubWorkflowNode("call-level-2", level2.definitionId()))
            .execute()
            .await().atMost(Duration.ofSeconds(10));
        
        // Execute root
        RunResponse run = client.runs()
            .create(level1.definitionId())
            .executeAndStart()
            .await().atMost(Duration.ofSeconds(10));
        
        RunResponse completed = waitForCompletion(run.runId());
        
        assertEquals("COMPLETED", completed.status());
        
        // Verify hierarchy
        WorkflowHierarchy hierarchy = client.workflows()
            .getHierarchy(run.runId())
            .await().atMost(Duration.ofSeconds(5));
        
        assertEquals(3, hierarchy.totalNodes());
        assertEquals(2, hierarchy.maxDepth());
    }
    
    @Test
    @Order(7)
    @DisplayName("Test parallel sub-workflows")
    void testParallelSubWorkflows() {
        WorkflowDefinitionResponse childA = createSimpleChildWorkflow("child-a");
        WorkflowDefinitionResponse childB = createSimpleChildWorkflow("child-b");
        WorkflowDefinitionResponse childC = createSimpleChildWorkflow("child-c");
        
        WorkflowDefinitionResponse parentWorkflow = client.workflows()
            .create("parallel-parent")
            .version("1.0.0")
            .addNode(createSubWorkflowNode("child-a-call", childA.definitionId()))
            .addNode(createSubWorkflowNode("child-b-call", childB.definitionId()))
            .addNode(createSubWorkflowNode("child-c-call", childC.definitionId()))
            .execute()
            .await().atMost(Duration.ofSeconds(10));
        
        RunResponse run = client.runs()
            .create(parentWorkflow.definitionId())
            .executeAndStart()
            .await().atMost(Duration.ofSeconds(10));
        
        RunResponse completed = waitForCompletion(run.runId());
        
        assertEquals("COMPLETED", completed.status());
        
        // Verify all children executed
        List<ChildWorkflowInfo> children = relationshipService
            .getChildWorkflows(
                WorkflowRunId.of(run.runId()),
                TenantId.of("test-tenant"))
            .await().atMost(Duration.ofSeconds(5));
        
        assertEquals(3, children.size());
    }
    
    @Test
    @Order(8)
    @DisplayName("Test fire-and-forget mode")
    void testFireAndForget() {
        WorkflowDefinitionResponse childWorkflow = createSlowWorkflow(Duration.ofSeconds(30));
        
        WorkflowDefinitionResponse parentWorkflow = client.workflows()
            .create("fire-forget-parent")
            .version("1.0.0")
            .addNode(createFireAndForgetNode("async-child", childWorkflow.definitionId()))
            .execute()
            .await().atMost(Duration.ofSeconds(10));
        
        long startTime = System.currentTimeMillis();
        
        RunResponse run = client.runs()
            .create(parentWorkflow.definitionId())
            .executeAndStart()
            .await().atMost(Duration.ofSeconds(10));
        
        RunResponse completed = waitForCompletion(run.runId());
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Parent should complete quickly (not wait for child)
        assertEquals("COMPLETED", completed.status());
        assertTrue(executionTime < 10000, "Parent should complete in < 10 seconds");
        
        // Verify child was launched
        assertEquals("DETACHED", 
            completed.nodeExecutions().get("async-child").output().get("status"));
    }
    
    @Test
    @Order(9)
    @DisplayName("Test cancellation cascade")
    void testCancellationCascade() {
        WorkflowDefinitionResponse child1 = createSlowWorkflow(Duration.ofSeconds(60));
        WorkflowDefinitionResponse child2 = createSlowWorkflow(Duration.ofSeconds(60));
        
        WorkflowDefinitionResponse parentWorkflow = client.workflows()
            .create("cascade-parent")
            .version("1.0.0")
            .addNode(createSubWorkflowNode("child1", child1.definitionId()))
            .addNode(createSubWorkflowNode("child2", child2.definitionId()))
            .execute()
            .await().atMost(Duration.ofSeconds(10));
        
        RunResponse run = client.runs()
            .create(parentWorkflow.definitionId())
            .executeAndStart()
            .await().atMost(Duration.ofSeconds(10));
        
        // Wait for children to start
        Thread.sleep(2000);
        
        // Cancel parent
        CascadeCancellationResult result = client.runs()
            .cancelCascade(run.runId(), "Test cancellation")
            .await().atMost(Duration.ofSeconds(10));
        
        assertTrue(result.success());
        assertEquals(3, result.workflowsCancelled()); // Parent + 2 children
    }
    
    // ==================== HELPER METHODS ====================
    
    private WorkflowDefinitionResponse createSimpleChildWorkflow() {
        return createSimpleChildWorkflow("simple-child");
    }
    
    private WorkflowDefinitionResponse createSimpleChildWorkflow(String name) {
        return client.workflows()
            .create(name)
            .version("1.0.0")
            .addNode(new NodeDefinitionDto(
                "simple-task",
                "Simple Task",
                "TASK",
                "test-executor",
                Map.of(),
                List.of(),
                List.of(),
                null, 10L, false
            ))
            .addInput("input1", stringInput("Input 1", false))
            .addOutput("output1", stringOutput("Output 1"))
            .execute()
            .await().atMost(Duration.ofSeconds(10));
    }
    
    private WorkflowDefinitionResponse createMappingTestWorkflow() {
        return client.workflows()
            .create("mapping-test-child")
            .version("1.0.0")
            .addNode(new NodeDefinitionDto(
                "transform",
                "Transform Data",
                "TASK",
                "transformation-executor",
                Map.of(),
                List.of(),
                List.of(),
                null, 10L, false
            ))
            .addInput("childField1", stringInput("Child Field 1", true))
            .addInput("childField2", stringInput("Child Field 2", true))
            .addOutput("childResult1", stringOutput("Child Result 1"))
            .addOutput("childResult2", stringOutput("Child Result 2"))
            .execute()
            .await().atMost(Duration.ofSeconds(10));
    }
    
    private WorkflowDefinitionResponse createFailingWorkflow() {
        return client.workflows()
            .create("failing-workflow")
            .version("1.0.0")
            .addNode(new NodeDefinitionDto(
                "fail-task",
                "Always Fails",
                "TASK",
                "failing-executor",
                Map.of("alwaysFail", true),
                List.of(),
                List.of(),
                null, 10L, false
            ))
            .execute()
            .await().atMost(Duration.ofSeconds(10));
    }
    
    private WorkflowDefinitionResponse createSlowWorkflow(Duration delay) {
        return client.workflows()
            .create("slow-workflow-" + delay.getSeconds())
            .version("1.0.0")
            .addNode(new NodeDefinitionDto(
                "delay-task",
                "Slow Task",
                "TASK",
                "delay-executor",
                Map.of("delaySeconds", delay.getSeconds()),
                List.of(),
                List.of(),
                null, delay.getSeconds() + 10, false
            ))
            .execute()
            .await().atMost(Duration.ofSeconds(10));
    }
    
    private NodeDefinitionDto createSubWorkflowNode(
            String nodeId,
            String childWorkflowId) {
        return createSubWorkflowNode(nodeId, childWorkflowId, Map.of(), Map.of());
    }
    
    private NodeDefinitionDto createSubWorkflowNode(
            String nodeId,
            String childWorkflowId,
            Map<String, String> inputMapping,
            Map<String, String> outputMapping) {
        return createSubWorkflowNode(nodeId, childWorkflowId, inputMapping, outputMapping, "PROPAGATE");
    }
    
    private NodeDefinitionDto createSubWorkflowNode(
            String nodeId,
            String childWorkflowId,
            Map<String, String> inputMapping,
            Map<String, String> outputMapping,
            String errorStrategy) {
        
        return new NodeDefinitionDto(
            nodeId,
            "Sub-Workflow: " + childWorkflowId,
            "SUB_WORKFLOW",
            "sub-workflow-executor",
            Map.of(
                "subWorkflowId", childWorkflowId,
                "inputMapping", inputMapping,
                "outputMapping", outputMapping,
                "waitForCompletion", true,
                "errorStrategy", errorStrategy
            ),
            List.of(),
            List.of(),
            null, 120L, false
        );
    }
    
    private NodeDefinitionDto createSubWorkflowNodeWithTimeout(
            String nodeId,
            String childWorkflowId,
            long timeoutSeconds) {
        
        return new NodeDefinitionDto(
            nodeId,
            "Sub-Workflow with Timeout",
            "SUB_WORKFLOW",
            "sub-workflow-executor",
            Map.of(
                "subWorkflowId", childWorkflowId,
                "waitForCompletion", true,
                "timeoutSeconds", timeoutSeconds,
                "errorStrategy", "PROPAGATE"
            ),
            List.of(),
            List.of(),
            null, timeoutSeconds + 10, false
        );
    }
    
    private NodeDefinitionDto createFireAndForgetNode(
            String nodeId,
            String childWorkflowId) {
        
        return new NodeDefinitionDto(
            nodeId,
            "Fire-and-Forget Sub-Workflow",
            "SUB_WORKFLOW",
            "sub-workflow-executor",
            Map.of(
                "subWorkflowId", childWorkflowId,
                "waitForCompletion", false
            ),
            List.of(),
            List.of(),
            null, 10L, false
        );
    }
    
    private InputDefinitionDto stringInput(String description, boolean required) {
        return new InputDefinitionDto(
            "string",
            "string",
            required,
            null,
            description
        );
    }
    
    private OutputDefinitionDto stringOutput(String description) {
        return new OutputDefinitionDto(
            "string",
            "string",
            description
        );
    }
    
    private RunResponse waitForCompletion(String runId) {
        int maxAttempts = 60; // 60 seconds max
        int attempts = 0;
        
        while (attempts < maxAttempts) {
            RunResponse run = client.runs()
                .get(runId)
                .await().atMost(Duration.ofSeconds(5));
            
            if (isTerminal(run.status())) {
                return run;
            }
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting", e);
            }
            
            attempts++;
        }
        
        throw new RuntimeException("Workflow did not complete within timeout");
    }
    
    private boolean isTerminal(String status) {
        return status.equals("COMPLETED") ||
               status.equals("FAILED") ||
               status.equals("CANCELLED");
    }
}

// ==================== REAL-WORLD USAGE EXAMPLES ====================

/**
 * Example 1: E-Commerce Order Processing with Fraud Detection
 * 
 * Demonstrates using sub-workflows for modularity and reusability.
 */
class ECommerceExample {
    
    public static void createOrderProcessingWorkflow(SilatClient client) {
        // Step 1: Create reusable fraud detection workflow
        WorkflowDefinitionResponse fraudDetection = client.workflows()
            .create("fraud-detection")
            .version("1.0.0")
            .description("Analyze transaction for fraud indicators")
            
            .addNode(new NodeDefinitionDto(
                "check-velocity",
                "Check Transaction Velocity",
                "TASK",
                "fraud-velocity-checker",
                Map.of(),
                List.of(),
                List.of(new TransitionDto("calculate-risk", null, "SUCCESS")),
                null, 30L, false
            ))
            
            .addNode(new NodeDefinitionDto(
                "calculate-risk",
                "Calculate Risk Score",
                "TASK",
                "risk-calculator",
                Map.of(),
                List.of("check-velocity"),
                List.of(),
                null, 30L, false
            ))
            
            .addInput("customerId", new InputDefinitionDto(
                "customerId", "string", true, null, "Customer ID"))
            .addInput("amount", new InputDefinitionDto(
                "amount", "number", true, null, "Transaction amount"))
            .addOutput("riskScore", new OutputDefinitionDto(
                "riskScore", "number", "Risk score 0-100"))
            .addOutput("approved", new OutputDefinitionDto(
                "approved", "boolean", "Whether approved"))
            
            .execute()
            .await().atMost(Duration.ofSeconds(10));
        
        // Step 2: Create main order processing workflow
        WorkflowDefinitionResponse orderProcessing = client.workflows()
            .create("order-processing")
            .version("2.0.0")
            .description("Complete order processing with fraud detection")
            
            .addNode(new NodeDefinitionDto(
                "validate-order",
                "Validate Order",
                "TASK",
                "order-validator",
                Map.of(),
                List.of(),
                List.of(new TransitionDto("fraud-check", null, "SUCCESS")),
                null, 30L, true
            ))
            
            // Sub-workflow node for fraud detection
            .addNode(new NodeDefinitionDto(
                "fraud-check",
                "Fraud Detection",
                "SUB_WORKFLOW",
                "sub-workflow-executor",
                Map.of(
                    "subWorkflowId", fraudDetection.definitionId(),
                    "waitForCompletion", true,
                    "timeoutSeconds", 60,
                    "inputMapping", Map.of(
                        "customerId", "customerId",
                        "amount", "totalAmount"
                    ),
                    "outputMapping", Map.of(
                        "fraudRiskScore", "riskScore",
                        "fraudApproved", "approved"
                    ),
                    "errorStrategy", "PROPAGATE"
                ),
                List.of("validate-order"),
                List.of(
                    new TransitionDto("process-payment", "fraudApproved == true", "CONDITION"),
                    new TransitionDto("manual-review", "fraudApproved == false", "CONDITION")
                ),
                null, 120L, true
            ))
            
            .addNode(new NodeDefinitionDto(
                "process-payment",
                "Process Payment",
                "TASK",
                "payment-processor",
                Map.of(),
                List.of("fraud-check"),
                List.of(new TransitionDto("fulfill-order", null, "SUCCESS")),
                null, 60L, true
            ))
            
            .addNode(new NodeDefinitionDto(
                "fulfill-order",
                "Fulfill Order",
                "TASK",
                "fulfillment-service",
                Map.of(),
                List.of("process-payment"),
                List.of(),
                null, 120L, false
            ))
            
            .addNode(new NodeDefinitionDto(
                "manual-review",
                "Manual Fraud Review",
                "HUMAN_TASK",
                "review-service",
                Map.of("role", "fraud-analyst"),
                List.of("fraud-check"),
                List.of(),
                null, 86400L, false // 24 hours
            ))
            
            .execute()
            .await().atMost(Duration.ofSeconds(10));
        
        System.out.println("Created order processing workflow: " + 
            orderProcessing.definitionId());
    }
}

/**
 * Example 2: Data Pipeline with Parallel Processing
 * 
 * Uses sub-workflows for parallel data processing stages.
 */
class DataPipelineExample {
    
    public static void createDataPipeline(SilatClient client) {
        // Create processing stage workflow
        WorkflowDefinitionResponse processingStage = client.workflows()
            .create("data-processing-stage")
            .version("1.0.0")
            .addNode(new NodeDefinitionDto(
                "transform-data",
                "Transform Data Chunk",
                "TASK",
                "data-transformer",
                Map.of(),
                List.of(),
                List.of(),
                null, 300L, false
            ))
            .addInput("chunkData", new InputDefinitionDto(
                "chunkData", "object", true, null, "Data chunk"))
            .addOutput("transformedData", new OutputDefinitionDto(
                "transformedData", "object", "Transformed data"))
            .execute()
            .await().atMost(Duration.ofSeconds(10));
        
        // Create main pipeline with parallel sub-workflows
        WorkflowDefinitionResponse pipeline = client.workflows()
            .create("parallel-data-pipeline")
            .version("1.0.0")
            
            .addNode(new NodeDefinitionDto(
                "split-data",
                "Split Data",
                "TASK",
                "data-splitter",
                Map.of("chunkSize", 1000),
                List.of(),
                List.of(
                    new TransitionDto("process-chunk-1", null, "SUCCESS"),
                    new TransitionDto("process-chunk-2", null, "SUCCESS"),
                    new TransitionDto("process-chunk-3", null, "SUCCESS")
                ),
                null, 60L, false
            ))
            
            // Parallel sub-workflow executions
            .addNode(new NodeDefinitionDto(
                "process-chunk-1",
                "Process Chunk 1",
                "SUB_WORKFLOW",
                "sub-workflow-executor",
                Map.of(
                    "subWorkflowId", processingStage.definitionId(),
                    "inputMapping", Map.of("chunkData", "chunk1")
                ),
                List.of("split-data"),
                List.of(new TransitionDto("merge-results", null, "SUCCESS")),
                null, 600L, false
            ))
            
            .addNode(new NodeDefinitionDto(
                "process-chunk-2",
                "Process Chunk 2",
                "SUB_WORKFLOW",
                "sub-workflow-executor",
                Map.of(
                    "subWorkflowId", processingStage.definitionId(),
                    "inputMapping", Map.of("chunkData", "chunk2")
                ),
                List.of("split-data"),
                List.of(new TransitionDto("merge-results", null, "SUCCESS")),
                null, 600L, false
            ))
            
            .addNode(new NodeDefinitionDto(
                "process-chunk-3",
                "Process Chunk 3",
                "SUB_WORKFLOW",
                "sub-workflow-executor",
                Map.of(
                    "subWorkflowId", processingStage.definitionId(),
                    "inputMapping", Map.of("chunkData", "chunk3")
                ),
                List.of("split-data"),
                List.of(new TransitionDto("merge-results", null, "SUCCESS")),
                null, 600L, false
            ))
            
            .addNode(new NodeDefinitionDto(
                "merge-results",
                "Merge Results",
                "AGGREGATE",
                "data-aggregator",
                Map.of(),
                List.of("process-chunk-1", "process-chunk-2", "process-chunk-3"),
                List.of(),
                null, 60L, false
            ))
            
            .execute()
            .await().atMost(Duration.ofSeconds(10));
        
        System.out.println("Created data pipeline: " + pipeline.definitionId());
    }
}


package tech.kayys.silat.enhancements;

/**
 * ============================================================================
 * SILAT WORKFLOW ENGINE - ENHANCEMENT ROADMAP
 * ============================================================================
 * 
 * Comprehensive list of enhancements to make Silat production-ready
 * and feature-complete for enterprise use.
 * 
 * Categories:
 * 1. Visual Workflow Designer & UI Schema
 * 2. Advanced Workflow Features
 * 3. Performance & Scalability
 * 4. Monitoring & Observability
 * 5. Security & Compliance
 * 6. Developer Experience
 * 7. AI/ML Integration
 * 8. Enterprise Integration
 * 9. DevOps & Operations
 * 10. Cloud-Native Features
 * 
 * @author Silat Team
 */

// ==================== 1. VISUAL WORKFLOW DESIGNER & UI SCHEMA ====================

/**
 * UI Node Schema for Visual Workflow Designer
 * 
 * Provides metadata for rendering nodes in a visual designer (React Flow, etc.)
 */
package tech.kayys.silat.ui.schema;

import jakarta.persistence.*;
import java.util.*;

/**
 * Node UI configuration schema
 * Used by frontend to render nodes correctly
 */
@Entity
@Table(name = "node_ui_schemas")
class NodeUISchema {
    
    @Id
    private String nodeType;
    
    @Column(columnDefinition = "jsonb")
    private UIMetadata metadata;
    
    @Column(columnDefinition = "jsonb")
    private List<UIPort> inputPorts;
    
    @Column(columnDefinition = "jsonb")
    private List<UIPort> outputPorts;
    
    @Column(columnDefinition = "jsonb")
    private UIConfiguration configuration;
    
    @Column(columnDefinition = "jsonb")
    private UIValidation validation;
}

/**
 * Complete UI metadata for visual designer
 */
record UIMetadata(
    String displayName,
    String category,          // "Task", "Control Flow", "Integration", "AI", etc.
    String icon,              // Icon identifier (lucide, fontawesome, etc.)
    String color,             // Primary color (hex)
    String backgroundColor,   // Background color
    String borderColor,       // Border color
    int defaultWidth,         // Default node width
    int defaultHeight,        // Default node height
    String description,       // User-facing description
    List<String> tags,        // Search tags
    boolean deprecated,       // Whether node type is deprecated
    String replacedBy         // If deprecated, what replaces it
) {}

/**
 * Port configuration (input/output connection points)
 */
record UIPort(
    String id,
    String label,
    PortType type,           // DATA, CONTROL, EVENT
    DataType dataType,       // STRING, NUMBER, OBJECT, ARRAY, ANY
    boolean required,
    boolean multiple,        // Can connect multiple edges
    String tooltip,
    PortPosition position,   // TOP, RIGHT, BOTTOM, LEFT
    Map<String, Object> customProperties
) {}

enum PortType {
    DATA,      // Data flow
    CONTROL,   // Control flow (success/failure transitions)
    EVENT      // Event-based connections
}

enum DataType {
    STRING,
    NUMBER,
    BOOLEAN,
    OBJECT,
    ARRAY,
    ANY,
    FILE,
    DATE,
    DURATION
}

enum PortPosition {
    TOP,
    RIGHT,
    BOTTOM,
    LEFT,
    AUTO
}

/**
 * Node configuration form schema (for properties panel)
 */
record UIConfiguration(
    List<ConfigField> fields,
    List<ConfigSection> sections,
    Map<String, ConditionalVisibility> conditionalFields
) {}

/**
 * Configuration field (rendered in properties panel)
 */
record ConfigField(
    String id,
    String label,
    FieldType type,
    Object defaultValue,
    boolean required,
    String placeholder,
    String tooltip,
    String helpText,
    FieldValidation validation,
    List<SelectOption> options,      // For select/radio
    Map<String, Object> fieldConfig  // Type-specific config
) {}

enum FieldType {
    TEXT,
    TEXTAREA,
    NUMBER,
    SELECT,
    MULTISELECT,
    CHECKBOX,
    RADIO,
    DATE,
    TIME,
    DURATION,
    JSON,
    CODE,              // Code editor with syntax highlighting
    KEY_VALUE,         // Key-value pair editor
    MAPPING,           // Input/output mapping editor
    FILE_UPLOAD,
    COLOR_PICKER,
    SLIDER,
    EXPRESSION,        // Expression builder
    WORKFLOW_SELECTOR, // Select another workflow (for sub-workflows)
    CREDENTIAL_SELECTOR // Select stored credentials
}

record SelectOption(
    String value,
    String label,
    String icon,
    String description
) {}

record FieldValidation(
    Integer minLength,
    Integer maxLength,
    Integer min,
    Integer max,
    String pattern,           // Regex pattern
    String customValidation,  // JavaScript validation function
    List<String> allowedValues
) {}

/**
 * Conditional field visibility
 */
record ConditionalVisibility(
    String dependsOn,      // Field ID this depends on
    String condition,      // JavaScript expression
    List<String> showWhen  // Values that trigger visibility
) {}

/**
 * Configuration section grouping
 */
record ConfigSection(
    String id,
    String label,
    String icon,
    boolean collapsible,
    boolean defaultExpanded,
    List<String> fields
) {}

/**
 * Validation rules for node configuration
 */
record UIValidation(
    List<ValidationRule> rules,
    String customValidator  // JavaScript validation function
) {}

record ValidationRule(
    String field,
    ValidationType type,
    String message,
    Map<String, Object> params
) {}

enum ValidationType {
    REQUIRED,
    MIN_LENGTH,
    MAX_LENGTH,
    PATTERN,
    MIN_VALUE,
    MAX_VALUE,
    CUSTOM
}

// ==================== UI SCHEMA EXAMPLES ====================

/**
 * Example: Sub-Workflow Node UI Schema
 */
class SubWorkflowNodeUISchema {
    
    public static NodeUISchema create() {
        return new NodeUISchema(
            "SUB_WORKFLOW",
            
            new UIMetadata(
                "Sub-Workflow",
                "Workflow",
                "workflow",              // Lucide icon name
                "#8B5CF6",              // Purple
                "#F3F4F6",
                "#8B5CF6",
                200,
                100,
                "Execute another workflow as a sub-workflow",
                List.of("workflow", "nested", "composite", "reusable"),
                false,
                null
            ),
            
            // Input ports
            List.of(
                new UIPort(
                    "in",
                    "In",
                    PortType.CONTROL,
                    DataType.ANY,
                    true,
                    false,
                    "Incoming connection",
                    PortPosition.LEFT,
                    Map.of()
                ),
                new UIPort(
                    "data-in",
                    "Data",
                    PortType.DATA,
                    DataType.OBJECT,
                    false,
                    true,
                    "Input data for sub-workflow",
                    PortPosition.LEFT,
                    Map.of()
                )
            ),
            
            // Output ports
            List.of(
                new UIPort(
                    "success",
                    "Success",
                    PortType.CONTROL,
                    DataType.ANY,
                    false,
                    false,
                    "Executes on successful completion",
                    PortPosition.RIGHT,
                    Map.of()
                ),
                new UIPort(
                    "failure",
                    "Failure",
                    PortType.CONTROL,
                    DataType.ANY,
                    false,
                    false,
                    "Executes on failure",
                    PortPosition.RIGHT,
                    Map.of()
                ),
                new UIPort(
                    "data-out",
                    "Data",
                    PortType.DATA,
                    DataType.OBJECT,
                    false,
                    false,
                    "Output data from sub-workflow",
                    PortPosition.RIGHT,
                    Map.of()
                )
            ),
            
            // Configuration
            new UIConfiguration(
                List.of(
                    new ConfigField(
                        "subWorkflowId",
                        "Workflow",
                        FieldType.WORKFLOW_SELECTOR,
                        null,
                        true,
                        "Select a workflow",
                        "The workflow to execute as a sub-workflow",
                        null,
                        new FieldValidation(null, null, null, null, null, null, null),
                        null,
                        Map.of()
                    ),
                    new ConfigField(
                        "waitForCompletion",
                        "Wait for Completion",
                        FieldType.CHECKBOX,
                        true,
                        false,
                        null,
                        "Wait for sub-workflow to complete before proceeding",
                        "If disabled, the sub-workflow runs asynchronously (fire-and-forget mode)",
                        null,
                        null,
                        Map.of()
                    ),
                    new ConfigField(
                        "timeoutSeconds",
                        "Timeout (seconds)",
                        FieldType.NUMBER,
                        3600,
                        false,
                        "3600",
                        "Maximum execution time",
                        null,
                        new FieldValidation(null, null, 1, 86400, null, null, null),
                        null,
                        Map.of("step", 1)
                    ),
                    new ConfigField(
                        "inputMapping",
                        "Input Mapping",
                        FieldType.MAPPING,
                        Map.of(),
                        false,
                        null,
                        "Map parent context to child workflow inputs",
                        "Format: childField -> parentField. Use dot notation for nested fields.",
                        null,
                        null,
                        Map.of(
                            "leftLabel", "Child Input",
                            "rightLabel", "Parent Field",
                            "allowExpressions", true
                        )
                    ),
                    new ConfigField(
                        "outputMapping",
                        "Output Mapping",
                        FieldType.MAPPING,
                        Map.of(),
                        false,
                        null,
                        "Map child workflow outputs to parent context",
                        "Format: parentField -> childOutput",
                        null,
                        null,
                        Map.of(
                            "leftLabel", "Parent Field",
                            "rightLabel", "Child Output"
                        )
                    ),
                    new ConfigField(
                        "errorStrategy",
                        "Error Handling",
                        FieldType.SELECT,
                        "PROPAGATE",
                        false,
                        null,
                        "How to handle errors from sub-workflow",
                        null,
                        null,
                        List.of(
                            new SelectOption("PROPAGATE", "Propagate Error", "alert-circle", 
                                "Fail parent node if child fails"),
                            new SelectOption("IGNORE", "Ignore Error", "check-circle", 
                                "Continue parent execution even if child fails"),
                            new SelectOption("RETRY_SUB_WORKFLOW", "Retry", "rotate-cw", 
                                "Retry the sub-workflow on failure"),
                            new SelectOption("CUSTOM", "Custom", "code", 
                                "Custom error handling logic")
                        ),
                        Map.of()
                    ),
                    new ConfigField(
                        "targetTenantId",
                        "Target Tenant",
                        FieldType.TEXT,
                        null,
                        false,
                        "Leave empty for same tenant",
                        "Execute sub-workflow in a different tenant (requires permissions)",
                        null,
                        null,
                        null,
                        Map.of()
                    ),
                    new ConfigField(
                        "passThroughContext",
                        "Pass Through Context",
                        FieldType.CHECKBOX,
                        false,
                        false,
                        null,
                        "Pass entire parent context to child",
                        "If enabled, all parent variables are available in child workflow",
                        null,
                        null,
                        Map.of()
                    )
                ),
                
                List.of(
                    new ConfigSection(
                        "basic",
                        "Basic Configuration",
                        "settings",
                        false,
                        true,
                        List.of("subWorkflowId", "waitForCompletion", "timeoutSeconds")
                    ),
                    new ConfigSection(
                        "mapping",
                        "Data Mapping",
                        "arrow-right-left",
                        true,
                        false,
                        List.of("inputMapping", "outputMapping", "passThroughContext")
                    ),
                    new ConfigSection(
                        "advanced",
                        "Advanced",
                        "sliders",
                        true,
                        false,
                        List.of("errorStrategy", "targetTenantId")
                    )
                ),
                
                Map.of(
                    "targetTenantId", new ConditionalVisibility(
                        "crossTenantEnabled",
                        "value === true",
                        null
                    ),
                    "timeoutSeconds", new ConditionalVisibility(
                        "waitForCompletion",
                        "value === true",
                        null
                    )
                )
            ),
            
            new UIValidation(
                List.of(
                    new ValidationRule(
                        "subWorkflowId",
                        ValidationType.REQUIRED,
                        "Workflow selection is required",
                        Map.of()
                    ),
                    new ValidationRule(
                        "timeoutSeconds",
                        ValidationType.MIN_VALUE,
                        "Timeout must be at least 1 second",
                        Map.of("min", 1)
                    )
                ),
                null
            )
        );
    }
}

/**
 * Example: HTTP Request Node UI Schema
 */
class HttpRequestNodeUISchema {
    
    public static NodeUISchema create() {
        return new NodeUISchema(
            "HTTP_REQUEST",
            
            new UIMetadata(
                "HTTP Request",
                "Integration",
                "globe",
                "#10B981",
                "#F3F4F6",
                "#10B981",
                180,
                80,
                "Make HTTP/REST API calls",
                List.of("http", "rest", "api", "integration", "webhook"),
                false,
                null
            ),
            
            List.of(
                new UIPort("in", "In", PortType.CONTROL, DataType.ANY, true, false, 
                    "Trigger", PortPosition.LEFT, Map.of())
            ),
            
            List.of(
                new UIPort("success", "Success", PortType.CONTROL, DataType.ANY, false, false,
                    "Request succeeded", PortPosition.RIGHT, Map.of()),
                new UIPort("failure", "Failure", PortType.CONTROL, DataType.ANY, false, false,
                    "Request failed", PortPosition.RIGHT, Map.of()),
                new UIPort("response", "Response", PortType.DATA, DataType.OBJECT, false, false,
                    "HTTP response", PortPosition.RIGHT, Map.of())
            ),
            
            new UIConfiguration(
                List.of(
                    new ConfigField(
                        "method",
                        "HTTP Method",
                        FieldType.SELECT,
                        "GET",
                        true,
                        null,
                        "HTTP request method",
                        null,
                        null,
                        List.of(
                            new SelectOption("GET", "GET", null, null),
                            new SelectOption("POST", "POST", null, null),
                            new SelectOption("PUT", "PUT", null, null),
                            new SelectOption("PATCH", "PATCH", null, null),
                            new SelectOption("DELETE", "DELETE", null, null)
                        ),
                        Map.of()
                    ),
                    new ConfigField(
                        "url",
                        "URL",
                        FieldType.TEXT,
                        null,
                        true,
                        "https://api.example.com/endpoint",
                        "Full URL of the endpoint",
                        "Supports expressions: ${variables.apiUrl}",
                        new FieldValidation(null, null, null, null, 
                            "^https?://.*", null, null),
                        null,
                        Map.of("expressions", true)
                    ),
                    new ConfigField(
                        "headers",
                        "Headers",
                        FieldType.KEY_VALUE,
                        Map.of(),
                        false,
                        null,
                        "HTTP headers",
                        null,
                        null,
                        null,
                        Map.of("valueExpressions", true)
                    ),
                    new ConfigField(
                        "body",
                        "Request Body",
                        FieldType.CODE,
                        null,
                        false,
                        "{}",
                        "Request body (JSON)",
                        null,
                        null,
                        null,
                        Map.of("language", "json", "height", 200)
                    ),
                    new ConfigField(
                        "authentication",
                        "Authentication",
                        FieldType.SELECT,
                        "NONE",
                        false,
                        null,
                        "Authentication method",
                        null,
                        null,
                        List.of(
                            new SelectOption("NONE", "None", null, null),
                            new SelectOption("BASIC", "Basic Auth", null, null),
                            new SelectOption("BEARER", "Bearer Token", null, null),
                            new SelectOption("OAUTH2", "OAuth 2.0", null, null),
                            new SelectOption("API_KEY", "API Key", null, null)
                        ),
                        Map.of()
                    ),
                    new ConfigField(
                        "credentials",
                        "Credentials",
                        FieldType.CREDENTIAL_SELECTOR,
                        null,
                        false,
                        null,
                        "Stored credentials",
                        null,
                        null,
                        null,
                        Map.of()
                    ),
                    new ConfigField(
                        "timeout",
                        "Timeout (ms)",
                        FieldType.NUMBER,
                        30000,
                        false,
                        "30000",
                        "Request timeout",
                        null,
                        new FieldValidation(null, null, 1000, 300000, null, null, null),
                        null,
                        Map.of()
                    ),
                    new ConfigField(
                        "retries",
                        "Max Retries",
                        FieldType.NUMBER,
                        3,
                        false,
                        "3",
                        "Maximum retry attempts",
                        null,
                        new FieldValidation(null, null, 0, 10, null, null, null),
                        null,
                        Map.of()
                    )
                ),
                
                List.of(
                    new ConfigSection("request", "Request", "send", false, true,
                        List.of("method", "url", "headers", "body")),
                    new ConfigSection("auth", "Authentication", "lock", true, false,
                        List.of("authentication", "credentials")),
                    new ConfigSection("options", "Options", "settings", true, false,
                        List.of("timeout", "retries"))
                ),
                
                Map.of(
                    "body", new ConditionalVisibility("method", null, 
                        List.of("POST", "PUT", "PATCH")),
                    "credentials", new ConditionalVisibility("authentication", null,
                        List.of("BASIC", "BEARER", "OAUTH2", "API_KEY"))
                )
            ),
            
            new UIValidation(
                List.of(
                    new ValidationRule("url", ValidationType.REQUIRED, 
                        "URL is required", Map.of()),
                    new ValidationRule("url", ValidationType.PATTERN,
                        "URL must be valid HTTP(S) URL", 
                        Map.of("pattern", "^https?://.*"))
                ),
                null
            )
        );
    }
}

// ==================== 2. ADVANCED WORKFLOW FEATURES ====================

/**
 * ENHANCEMENT: Dynamic Workflow Generation
 * 
 * Generate workflows programmatically based on templates or AI
 */
interface DynamicWorkflowGenerator {
    
    /**
     * Generate workflow from template with parameters
     */
    Uni<WorkflowDefinition> generateFromTemplate(
        String templateId,
        Map<String, Object> parameters,
        TenantId tenantId
    );
    
    /**
     * Generate workflow from natural language description (AI)
     */
    Uni<WorkflowDefinition> generateFromDescription(
        String description,
        TenantId tenantId
    );
    
    /**
     * Clone and modify existing workflow
     */
    Uni<WorkflowDefinition> cloneAndModify(
        WorkflowDefinitionId sourceId,
        List<WorkflowModification> modifications,
        TenantId tenantId
    );
}

/**
 * ENHANCEMENT: Workflow Versioning & Blue-Green Deployments
 */
interface WorkflowVersioningService {
    
    /**
     * Create new version of workflow
     */
    Uni<WorkflowVersion> createVersion(
        WorkflowDefinitionId workflowId,
        String version,
        VersionType type, // MAJOR, MINOR, PATCH
        String changeLog
    );
    
    /**
     * Deploy version with traffic splitting
     */
    Uni<Deployment> deployVersion(
        WorkflowDefinitionId workflowId,
        String version,
        TrafficSplit split // e.g., 90% old, 10% new
    );
    
    /**
     * Rollback to previous version
     */
    Uni<Void> rollback(
        WorkflowDefinitionId workflowId,
        String toVersion
    );
    
    /**
     * Compare two versions
     */
    Uni<VersionComparison> compareVersions(
        WorkflowDefinitionId workflowId,
        String version1,
        String version2
    );
}

/**
 * ENHANCEMENT: Workflow Testing Framework
 */
interface WorkflowTestingService {
    
    /**
     * Create test case for workflow
     */
    Uni<WorkflowTestCase> createTestCase(
        WorkflowDefinitionId workflowId,
        String name,
        Map<String, Object> inputs,
        Map<String, Object> expectedOutputs,
        List<NodeAssertion> nodeAssertions
    );
    
    /**
     * Run test suite
     */
    Uni<TestSuiteResult> runTestSuite(
        WorkflowDefinitionId workflowId,
        List<String> testCaseIds
    );
    
    /**
     * Generate test cases from execution history
     */
    Uni<List<WorkflowTestCase>> generateTestsFromHistory(
        WorkflowDefinitionId workflowId,
        int sampleSize
    );
    
    /**
     * Mock external dependencies
     */
    Uni<Void> createMock(
        String nodeId,
        MockBehavior behavior
    );
}

/**
 * ENHANCEMENT: Workflow Simulation & What-If Analysis
 */
interface WorkflowSimulationService {
    
    /**
     * Simulate workflow execution without actually running it
     */
    Uni<SimulationResult> simulate(
        WorkflowDefinitionId workflowId,
        Map<String, Object> inputs,
        SimulationConfig config
    );
    
    /**
     * Estimate cost and duration
     */
    Uni<ExecutionEstimate> estimateExecution(
        WorkflowDefinitionId workflowId,
        Map<String, Object> inputs
    );
    
    /**
     * What-if analysis: "What if this node fails?"
     */
    Uni<WhatIfResult> whatIfAnalysis(
        WorkflowDefinitionId workflowId,
        List<WhatIfScenario> scenarios
    );
}

/**
 * ENHANCEMENT: Workflow Optimization & Recommendations
 */
interface WorkflowOptimizationService {
    
    /**
     * Analyze workflow for performance bottlenecks
     */
    Uni<OptimizationReport> analyzeWorkflow(
        WorkflowDefinitionId workflowId
    );
    
    /**
     * Suggest optimizations
     */
    Uni<List<Optimization>> suggestOptimizations(
        WorkflowDefinitionId workflowId
    );
    
    /**
     * Auto-optimize workflow
     */
    Uni<WorkflowDefinition> autoOptimize(
        WorkflowDefinitionId workflowId,
        OptimizationGoals goals
    );
}

/**
 * ENHANCEMENT: Workflow Marketplace
 */
interface WorkflowMarketplaceService {
    
    /**
     * Publish workflow to marketplace
     */
    Uni<MarketplaceListing> publishWorkflow(
        WorkflowDefinitionId workflowId,
        ListingDetails details
    );
    
    /**
     * Search marketplace
     */
    Uni<List<MarketplaceListing>> searchMarketplace(
        String query,
        List<String> categories,
        int page,
        int size
    );
    
    /**
     * Install workflow from marketplace
     */
    Uni<WorkflowDefinition> installFromMarketplace(
        String listingId,
        TenantId tenantId
    );
    
    /**
     * Rate and review
     */
    Uni<Void> rateWorkflow(
        String listingId,
        int rating,
        String review
    );
}

// ==================== 3. PERFORMANCE & SCALABILITY ====================

/**
 * ENHANCEMENT: Workflow Caching Strategy
 */
interface WorkflowCacheService {
    
    /**
     * Cache workflow definition
     */
    Uni<Void> cacheDefinition(WorkflowDefinition definition);
    
    /**
     * Cache execution results (memoization)
     */
    Uni<Void> cacheExecutionResult(
        WorkflowRunId runId,
        NodeId nodeId,
        Map<String, Object> inputs,
        Map<String, Object> outputs
    );
    
    /**
     * Get cached result
     */
    Uni<Optional<Map<String, Object>>> getCachedResult(
        WorkflowDefinitionId workflowId,
        NodeId nodeId,
        Map<String, Object> inputs
    );
    
    /**
     * Invalidate cache
     */
    Uni<Void> invalidateCache(WorkflowDefinitionId workflowId);
}

/**
 * ENHANCEMENT: Batch Processing
 */
interface BatchWorkflowProcessor {
    
    /**
     * Submit batch of workflow executions
     */
    Uni<BatchSubmission> submitBatch(
        WorkflowDefinitionId workflowId,
        List<Map<String, Object>> inputsList,
        BatchConfig config
    );
    
    /**
     * Monitor batch progress
     */
    Uni<BatchProgress> getBatchProgress(String batchId);
    
    /**
     * Cancel entire batch
     */
    Uni<Void> cancelBatch(String batchId);
}

/**
 * ENHANCEMENT: Workflow Sharding & Partitioning
 */
interface WorkflowShardingService {
    
    /**
     * Distribute workflow runs across shards
     */
    Uni<ShardAssignment> assignShard(WorkflowRunId runId);
    
    /**
     * Rebalance shards
     */
    Uni<Void> rebalanceShards();
    
    /**
     * Get shard statistics
     */
    Uni<List<ShardStats>> getShardStats();
}

// ==================== 4. MONITORING & OBSERVABILITY ====================

/**
 * ENHANCEMENT: Real-Time Workflow Metrics
 */
interface WorkflowMetricsService {
    
    /**
     * Record custom metric
     */
    void recordMetric(
        WorkflowRunId runId,
        String metricName,
        double value,
        Map<String, String> tags
    );
    
    /**
     * Query metrics
     */
    Uni<List<MetricData>> queryMetrics(
        MetricQuery query
    );
    
    /**
     * Create dashboard
     */
    Uni<Dashboard> createDashboard(
        String name,
        List<Widget> widgets
    );
}

/**
 * ENHANCEMENT: Distributed Tracing Integration
 */
interface WorkflowTracingService {
    
    /**
     * Start trace span for workflow
     */
    Span startWorkflowSpan(WorkflowRunId runId);
    
    /**
     * Start trace span for node
     */
    Span startNodeSpan(WorkflowRunId runId, NodeId nodeId);
    
    /**
     * Add trace attributes
     */
    void addTraceAttribute(String key, String value);
    
    /**
     * Get trace context
     */
    TraceContext getTraceContext();
}

/**
 * ENHANCEMENT: Workflow Anomaly Detection
 */
interface AnomalyDetectionService {
    
    /**
     * Detect anomalies in workflow execution
     */
    Uni<List<Anomaly>> detectAnomalies(
        WorkflowDefinitionId workflowId,
        Duration timeWindow
    );
    
    /**
     * Create anomaly alert
     */
    Uni<Void> createAlert(
        AnomalyType type,
        AlertConfig config
    );
    
    /**
     * Train anomaly detection model
     */
    Uni<Void> trainModel(
        WorkflowDefinitionId workflowId,
        Duration trainingPeriod
    );
}

// ==================== 5. SECURITY & COMPLIANCE ====================

/**
 * ENHANCEMENT: Workflow Encryption
 */
interface WorkflowEncryptionService {
    
    /**
     * Encrypt sensitive workflow data
     */
    Uni<EncryptedData> encrypt(
        WorkflowRunId runId,
        String data,
        EncryptionConfig config
    );
    
    /**
     * Decrypt workflow data
     */
    Uni<String> decrypt(
        WorkflowRunId runId,
        EncryptedData encryptedData
    );
    
    /**
     * Rotate encryption keys
     */
    Uni<Void> rotateKeys(TenantId tenantId);
}

/**
 * ENHANCEMENT: Workflow Audit & Compliance
 */
interface WorkflowComplianceService {
    
    /**
     * Generate compliance report
     */
    Uni<ComplianceReport> generateReport(
        TenantId tenantId,
        ComplianceStandard standard, // SOC2, HIPAA, GDPR, etc.
        Instant startDate,
        Instant endDate
    );
    
    /**
     * Check workflow compliance
     */
    Uni<ComplianceResult> checkCompliance(
        WorkflowDefinitionId workflowId,
        List<ComplianceRule> rules
    );
    
    /**
     * Data retention enforcement
     */
    Uni<Void> enforceRetention(
        RetentionPolicy policy
    );
}

/**
 * ENHANCEMENT: Fine-Grained Access Control (RBAC/ABAC)
 */
interface WorkflowAccessControlService {
    
    /**
     * Define role-based permissions
     */
    Uni<Void> defineRole(
        String roleName,
        List<Permission> permissions
    );
    
    /**
     * Assign role to user
     */
    Uni<Void> assignRole(
        String userId,
        String roleName,
        TenantId tenantId
    );
    
    /**
     * Check permission
     */
    Uni<Boolean> hasPermission(
        String userId,
        Permission permission,
        WorkflowDefinitionId workflowId
    );
    
    /**
     * Attribute-based access control
     */
    Uni<Boolean> evaluatePolicy(
        String userId,
        Map<String, Object> context,
        AccessPolicy policy
    );
}

// ==================== 6. DEVELOPER EXPERIENCE ====================

/**
 * ENHANCEMENT: Workflow CLI
 */
interface WorkflowCLI {
    
    /**
     * Initialize new workflow project
     */
    void init(String projectName, String template);
    
    /**
     * Deploy workflow
     */
    void deploy(String workflowFile, String environment);
    
    /**
     * Run workflow locally
     */
    void run(String workflowId, String inputFile);
    
    /**
     * Generate client SDK
     */
    void generateSDK(String workflowId, String language);
    
    /**
     * Workflow linting
     */
    void lint(String workflowFile);
}

/**
 * ENHANCEMENT: Workflow as Code (YAML/JSON DSL)
 */
record WorkflowDSL(
    String name,
    String version,
    Map<String, Input> inputs,
    Map<String, Output> outputs,
    List<NodeDSL> nodes,
    WorkflowConfig config
) {
    /**
     * Example YAML:
     * 
     * name: order-processing
     * version: 1.0.0
     * 
     * inputs:
     *   orderId:
     *     type: string
     *     required: true
     *   customerId:
     *     type: string
     *     required: true
     * 
     * outputs:
     *   transactionId:
     *     type: string
     *   trackingNumber:
     *     type: string
     * 
     * nodes:
     *   - id: validate-order
     *     type: TASK
     *     executor: order-validator
     *     transitions:
     *       - to: fraud-check
     *         condition: success
     * 
     *   - id: fraud-check
     *     type: SUB_WORKFLOW
     *     executor: sub-workflow-executor
     *     config:
     *       subWorkflowId: fraud-detection
     *       inputMapping:
     *         customerId: customerId
     *         amount: totalAmount
     *     transitions:
     *       - to: process-payment
     *         condition: fraudApproved == true
     * 
     *   - id: process-payment
     *     type: TASK
     *     executor: payment-processor
     *     retry:
     *       maxAttempts: 5
     *       backoffMultiplier: 2.0
     * 
     * config:
     *   timeout: 1h
     *   retryPolicy:
     *     maxAttempts: 3
     *   compensationPolicy:
     *     strategy: SEQUENTIAL
     */
}

/**
 * ENHANCEMENT: Hot Reload & Live Development
 */
interface LiveDevelopmentService {
    
    /**
     * Enable hot reload for workflow
     */
    Uni<Void> enableHotReload(WorkflowDefinitionId workflowId);
    
    /**
     * Update workflow definition without stopping runs
     */
    Uni<Void> hotUpdate(
        WorkflowDefinitionId workflowId,
        WorkflowDefinition newDefinition
    );
    
    /**
     * Debug mode with breakpoints
     */
    Uni<DebugSession> startDebugSession(WorkflowRunId runId);
}

/**
 * ENHANCEMENT: Workflow Documentation Generator
 */
interface WorkflowDocumentationService {
    
    /**
     * Generate documentation from workflow
     */
    Uni<Documentation> generateDocs(
        WorkflowDefinitionId workflowId,
        DocumentationFormat format // MARKDOWN, HTML, PDF
    );
    
    /**
     * Generate API documentation
     */
    Uni<APIDocumentation> generateAPIDocs(
        WorkflowDefinitionId workflowId,
        APIDocFormat format // OPENAPI, ASYNCAPI
    );
    
    /**
     * Generate visual diagrams
     */
    Uni<byte[]> generateDiagram(
        WorkflowDefinitionId workflowId,
        DiagramType type // FLOWCHART, SEQUENCE, BPMN
    );
}

// ==================== 7. AI/ML INTEGRATION ====================

/**
 * ENHANCEMENT: ML-Powered Workflow Optimization
 */
interface MLWorkflowOptimizer {
    
    /**
     * Predict workflow execution time
     */
    Uni<Duration> predictExecutionTime(
        WorkflowDefinitionId workflowId,
        Map<String, Object> inputs
    );
    
    /**
     * Recommend optimal execution path
     */
    Uni<List<NodeId>> recommendExecutionPath(
        WorkflowDefinitionId workflowId,
        Map<String, Object> context
    );
    
    /**
     * Predict failure probability
     */
    Uni<FailurePrediction> predictFailure(
        WorkflowRunId runId
    );
    
    /**
     * Auto-tune retry policies based on history
     */
    Uni<RetryPolicy> optimizeRetryPolicy(
        WorkflowDefinitionId workflowId,
        NodeId nodeId
    );
}

/**
 * ENHANCEMENT: Natural Language Workflow Builder
 */
interface NLPWorkflowBuilder {
    
    /**
     * Build workflow from natural language
     */
    Uni<WorkflowDefinition> buildFromNL(
        String description,
        TenantId tenantId
    );
    
    /**
     * Suggest next node from context
     */
    Uni<List<NodeSuggestion>> suggestNextNode(
        WorkflowDefinitionId workflowId,
        NodeId currentNode,
        String intent
    );
    
    /**
     * Explain workflow in natural language
     */
    Uni<String> explainWorkflow(
        WorkflowDefinitionId workflowId
    );
}

/**
 * ENHANCEMENT: Intelligent Error Recovery
 */
interface IntelligentErrorRecovery {
    
    /**
     * Suggest error recovery actions
     */
    Uni<List<RecoveryAction>> suggestRecovery(
        WorkflowRunId runId,
        ErrorInfo error
    );
    
    /**
     * Auto-recover from common errors
     */
    Uni<Void> autoRecover(
        WorkflowRunId runId,
        RecoveryStrategy strategy
    );
    
    /**
     * Learn from error patterns
     */
    Uni<Void> learnFromErrors(
        WorkflowDefinitionId workflowId,
        Duration learningPeriod
    );
}

// ==================== 8. ENTERPRISE INTEGRATION ====================

/**
 * ENHANCEMENT: Enterprise Service Bus Integration
 */
interface ESBIntegrationService {
    
    /**
     * Connect to ESB
     */
    Uni<Void> connectESB(
        ESBType type, // MULESOFT, TIBCO, etc.
        ESBConfig config
    );
    
    /**
     * Expose workflow as ESB service
     */
    Uni<ServiceEndpoint> exposeAsService(
        WorkflowDefinitionId workflowId,
        ServiceConfig config
    );
    
    /**
     * Consume ESB service in workflow
     */
    Uni<NodeDefinition> createESBNode(
        String serviceName,
        ESBOperationConfig config
    );
}

/**
 * ENHANCEMENT: Legacy System Connectors
 */
interface LegacySystemConnectorService {
    
    /**
     * SAP connector
     */
    Uni<SAPConnection> connectSAP(SAPConfig config);
    
    /**
     * Mainframe connector (CICS, IMS)
     */
    Uni<MainframeConnection> connectMainframe(MainframeConfig config);
    
    /**
     * Oracle EBS connector
     */
    Uni<OracleEBSConnection> connectOracleEBS(OracleEBSConfig config);
    
    /**
     * AS400 connector
     */
    Uni<AS400Connection> connectAS400(AS400Config config);
}

/**
 * ENHANCEMENT: EDI/B2B Integration
 */
interface B2BIntegrationService {
    
    /**
     * Parse EDI message
     */
    Uni<Map<String, Object>> parseEDI(
        String ediMessage,
        EDIStandard standard // X12, EDIFACT
    );
    
    /**
     * Generate EDI message
     */
    Uni<String> generateEDI(
        Map<String, Object> data,
        EDIStandard standard
    );
    
    /**
     * AS2/SFTP file transfer
     */
    Uni<Void> transferFile(
        byte[] fileData,
        B2BProtocol protocol,
        B2BConfig config
    );
}

// ==================== 9. DEVOPS & OPERATIONS ====================

/**
 * ENHANCEMENT: GitOps Workflow Management
 */
interface GitOpsWorkflowService {
    
    /**
     * Sync workflows from Git repository
     */
    Uni<Void> syncFromGit(
        String repositoryUrl,
        String branch,
        TenantId tenantId
    );
    
    /**
     * Deploy from Git tag/commit
     */
    Uni<Deployment> deployFromGit(
        String repositoryUrl,
        String ref, // tag or commit SHA
        TenantId tenantId
    );
    
    /**
     * Enable auto-sync
     */
    Uni<Void> enableAutoSync(
        String repositoryUrl,
        Duration syncInterval
    );
}

/**
 * ENHANCEMENT: Chaos Engineering for Workflows
 */
interface WorkflowChaosService {
    
    /**
     * Inject failure into node
     */
    Uni<Void> injectFailure(
        WorkflowRunId runId,
        NodeId nodeId,
        FailureType type,
        Duration duration
    );
    
    /**
     * Inject latency
     */
    Uni<Void> injectLatency(
        WorkflowRunId runId,
        NodeId nodeId,
        Duration latency
    );
    
    /**
     * Simulate network partition
     */
    Uni<Void> simulateNetworkPartition(
        List<String> isolatedNodes,
        Duration duration
    );
    
    /**
     * Run chaos experiment
     */
    Uni<ChaosResult> runExperiment(
        ChaosExperiment experiment
    );
}

/**
 * ENHANCEMENT: Workflow Disaster Recovery
 */
interface DisasterRecoveryService {
    
    /**
     * Create backup
     */
    Uni<BackupId> createBackup(
        TenantId tenantId,
        BackupScope scope
    );
    
    /**
     * Restore from backup
     */
    Uni<Void> restoreFromBackup(
        BackupId backupId,
        TenantId tenantId
    );
    
    /**
     * Setup cross-region replication
     */
    Uni<Void> setupReplication(
        String primaryRegion,
        String secondaryRegion,
        ReplicationConfig config
    );
    
    /**
     * Failover to secondary region
     */
    Uni<Void> failover(
        String toRegion,
        FailoverMode mode // MANUAL, AUTOMATIC
    );
}

// ==================== 10. CLOUD-NATIVE FEATURES ====================

/**
 * ENHANCEMENT: Kubernetes Native Integration
 */
interface K8sWorkflowService {
    
    /**
     * Deploy workflow as Kubernetes CRD
     */
    Uni<Void> deployAsCRD(
        WorkflowDefinition workflow,
        String namespace
    );
    
    /**
     * Scale executors based on load
     */
    Uni<Void> autoScale(
        ScalingPolicy policy
    );
    
    /**
     * Deploy to Kubernetes cluster
     */
    Uni<Deployment> deployToK8s(
        WorkflowDefinitionId workflowId,
        K8sDeploymentConfig config
    );
}

/**
 * ENHANCEMENT: Serverless Workflow Execution
 */
interface ServerlessWorkflowService {
    
    /**
     * Execute workflow on AWS Lambda
     */
    Uni<WorkflowRun> executeOnLambda(
        WorkflowDefinitionId workflowId,
        Map<String, Object> inputs,
        LambdaConfig config
    );
    
    /**
     * Execute on Google Cloud Functions
     */
    Uni<WorkflowRun> executeOnGCF(
        WorkflowDefinitionId workflowId,
        Map<String, Object> inputs,
        GCFConfig config
    );
    
    /**
     * Execute on Azure Functions
     */
    Uni<WorkflowRun> executeOnAzure(
        WorkflowDefinitionId workflowId,
        Map<String, Object> inputs,
        AzureConfig config
    );
}

/**
 * ENHANCEMENT: Multi-Cloud Support
 */
interface MultiCloudService {
    
    /**
     * Deploy to multiple clouds
     */
    Uni<List<Deployment>> deployMultiCloud(
        WorkflowDefinitionId workflowId,
        List<CloudProvider> providers
    );
    
    /**
     * Load balance across clouds
     */
    Uni<Void> configureLoadBalancing(
        LoadBalancingStrategy strategy
    );
    
    /**
     * Cloud cost optimization
     */
    Uni<CostReport> optimizeCloudCosts(
        TenantId tenantId,
        Duration period
    );
}

// ==================== PRIORITY ENHANCEMENTS ====================

/**
 * RECOMMENDED IMPLEMENTATION PRIORITY
 * 
 * Phase 1 (Core UX - 2-3 months):
 * ✅ 1. Visual Workflow Designer UI Schema (CRITICAL)
 * ✅ 2. Workflow as Code (YAML/JSON DSL)
 * ✅ 3. Workflow Testing Framework
 * ✅ 4. Enhanced Monitoring & Metrics
 * ✅ 5. Workflow Documentation Generator
 * 
 * Phase 2 (Enterprise Features - 2-3 months):
 * ✅ 6. Workflow Versioning & Deployments
 * ✅ 7. Advanced RBAC/ABAC
 * ✅ 8. Workflow Marketplace
 * ✅ 9. Batch Processing
 * ✅ 10. GitOps Integration
 * 
 * Phase 3 (AI/ML - 2-3 months):
 * ✅ 11. ML-Powered Optimization
 * ✅ 12. NLP Workflow Builder
 * ✅ 13. Intelligent Error Recovery
 * ✅ 14. Anomaly Detection
 * 
 * Phase 4 (Scale & Performance - 2-3 months):
 * ✅ 15. Workflow Caching
 * ✅ 16. Sharding & Partitioning
 * ✅ 17. Distributed Tracing
 * ✅ 18. Chaos Engineering
 * ✅ 19. Disaster Recovery
 * 
 * Phase 5 (Cloud Native - 2-3 months):
 * ✅ 20. Kubernetes Native
 * ✅ 21. Serverless Support
 * ✅ 22. Multi-Cloud
 * ✅ 23. Cost Optimization
 */

/**
 * IMMEDIATE PRIORITIES FOR NEXT SPRINT
 * 
 * 1. UI Node Schema Implementation (THIS FILE)
 *    - Define schemas for all built-in node types
 *    - Create UI schema registry
 *    - Build schema validation
 *    - REST API for schema retrieval
 * 
 * 2. Workflow as Code (YAML DSL)
 *    - YAML parser/serializer
 *    - DSL to WorkflowDefinition converter
 *    - CLI tool for workflow management
 *    - Validation and linting
 * 
 * 3. Enhanced Metrics & Monitoring
 *    - Prometheus metrics exporter
 *    - Custom metric recording
 *    - Real-time dashboards
 *    - Alert configuration
 * 
 * 4. Testing Framework
 *    - Test case management
 *    - Mock service for executors
 *    - Assertion framework
 *    - Test suite runner
 * 
 * 5. Documentation Generator
 *    - Workflow to Markdown
 *    - OpenAPI spec generation
 *    - Diagram generation (Mermaid)
 *    - Interactive docs
 */

 package tech.kayys.silat.ui;

import io.quarkus.runtime.Startup;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ============================================================================
 * UI SCHEMA REGISTRY - COMPLETE IMPLEMENTATION
 * ============================================================================
 * 
 * Central registry for all node type UI schemas.
 * Provides schemas to frontend for rendering visual workflow designer.
 * 
 * Features:
 * - Built-in node type schemas
 * - Custom node type registration
 * - Schema versioning
 * - Schema validation
 * - Hot reload support
 * 
 * @author Silat Team
 */

// ==================== UI SCHEMA REGISTRY ====================

@ApplicationScoped
@Startup
public class NodeUISchemaRegistry {
    
    private static final org.slf4j.Logger LOG = 
        org.slf4j.LoggerFactory.getLogger(NodeUISchemaRegistry.class);
    
    private final Map<String, NodeUISchema> schemas = new ConcurrentHashMap<>();
    
    @PostConstruct
    void initialize() {
        LOG.info("Initializing Node UI Schema Registry");
        
        // Register all built-in node types
        registerBuiltInSchemas();
        
        LOG.info("Registered {} node type schemas", schemas.size());
    }
    
    /**
     * Register a node schema
     */
    public void register(NodeUISchema schema) {
        schemas.put(schema.nodeType(), schema);
        LOG.info("Registered schema for node type: {}", schema.nodeType());
    }
    
    /**
     * Get schema by node type
     */
    public Optional<NodeUISchema> getSchema(String nodeType) {
        return Optional.ofNullable(schemas.get(nodeType));
    }
    
    /**
     * Get all schemas
     */
    public Map<String, NodeUISchema> getAllSchemas() {
        return new HashMap<>(schemas);
    }
    
    /**
     * Get schemas by category
     */
    public Map<String, NodeUISchema> getSchemasByCategory(String category) {
        return schemas.values().stream()
            .filter(schema -> schema.metadata().category().equals(category))
            .collect(java.util.stream.Collectors.toMap(
                NodeUISchema::nodeType,
                schema -> schema
            ));
    }
    
    /**
     * Search schemas
     */
    public List<NodeUISchema> searchSchemas(String query) {
        String lowerQuery = query.toLowerCase();
        return schemas.values().stream()
            .filter(schema -> 
                schema.metadata().displayName().toLowerCase().contains(lowerQuery) ||
                schema.metadata().description().toLowerCase().contains(lowerQuery) ||
                schema.metadata().tags().stream()
                    .anyMatch(tag -> tag.toLowerCase().contains(lowerQuery))
            )
            .toList();
    }
    
    /**
     * Register all built-in node types
     */
    private void registerBuiltInSchemas() {
        // Core node types
        register(TaskNodeSchema.create());
        register(DecisionNodeSchema.create());
        register(ParallelNodeSchema.create());
        register(AggregateNodeSchema.create());
        register(SubWorkflowNodeSchema.create());
        
        // Human interaction
        register(HumanTaskNodeSchema.create());
        register(ApprovalNodeSchema.create());
        
        // Integration
        register(HttpRequestNodeSchema.create());
        register(RestAPINodeSchema.create());
        register(GraphQLNodeSchema.create());
        register(GrpcCallNodeSchema.create());
        
        // Data
        register(DatabaseQueryNodeSchema.create());
        register(DataTransformNodeSchema.create());
        register(JsonPathNodeSchema.create());
        
        // Messaging
        register(KafkaProducerNodeSchema.create());
        register(KafkaConsumerNodeSchema.create());
        register(RabbitMQNodeSchema.create());
        
        // Cloud
        register(AwsLambdaNodeSchema.create());
        register(S3OperationNodeSchema.create());
        register(GcpStorageNodeSchema.create());
        
        // AI/ML
        register(LLMCallNodeSchema.create());
        register(EmbeddingNodeSchema.create());
        register(VectorSearchNodeSchema.create());
        
        // Utilities
        register(TimerNodeSchema.create());
        register(EventWaitNodeSchema.create());
        register(ScriptNodeSchema.create());
        register(EmailNodeSchema.create());
    }
}

// ==================== REST API ====================

@Path("/api/v1/ui/schemas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "UI Schemas", description = "Node UI schema registry")
public class NodeUISchemaResource {
    
    @Inject
    NodeUISchemaRegistry registry;
    
    @GET
    @Operation(summary = "Get all node schemas")
    public Uni<Map<String, NodeUISchema>> getAllSchemas() {
        return Uni.createFrom().item(registry.getAllSchemas());
    }
    
    @GET
    @Path("/{nodeType}")
    @Operation(summary = "Get schema for specific node type")
    public Uni<RestResponse<NodeUISchema>> getSchema(
            @PathParam("nodeType") String nodeType) {
        
        return Uni.createFrom().item(
            registry.getSchema(nodeType)
                .map(RestResponse::ok)
                .orElse(RestResponse.notFound())
        );
    }
    
    @GET
    @Path("/category/{category}")
    @Operation(summary = "Get schemas by category")
    public Uni<Map<String, NodeUISchema>> getSchemasByCategory(
            @PathParam("category") String category) {
        
        return Uni.createFrom().item(
            registry.getSchemasByCategory(category)
        );
    }
    
    @GET
    @Path("/search")
    @Operation(summary = "Search schemas")
    public Uni<List<NodeUISchema>> searchSchemas(
            @QueryParam("q") String query) {
        
        return Uni.createFrom().item(
            registry.searchSchemas(query != null ? query : "")
        );
    }
    
    @GET
    @Path("/categories")
    @Operation(summary = "Get all categories")
    public Uni<List<CategoryInfo>> getCategories() {
        Map<String, Integer> categoryCounts = new HashMap<>();
        
        registry.getAllSchemas().values().forEach(schema -> {
            String category = schema.metadata().category();
            categoryCounts.merge(category, 1, Integer::sum);
        });
        
        List<CategoryInfo> categories = categoryCounts.entrySet().stream()
            .map(e -> new CategoryInfo(e.getKey(), e.getValue()))
            .sorted(Comparator.comparing(CategoryInfo::name))
            .toList();
        
        return Uni.createFrom().item(categories);
    }
}

record CategoryInfo(String name, int count) {}

// ==================== BUILT-IN NODE SCHEMAS ====================

/**
 * Task Node Schema
 */
class TaskNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema(
            "TASK",
            new UIMetadata(
                "Task",
                "Core",
                "box",
                "#3B82F6",
                "#EFF6FF",
                "#3B82F6",
                160,
                80,
                "Execute a task or action",
                List.of("task", "action", "execute"),
                false,
                null
            ),
            List.of(
                new UIPort("in", "In", PortType.CONTROL, DataType.ANY, 
                    true, false, "Trigger", PortPosition.LEFT, Map.of())
            ),
            List.of(
                new UIPort("success", "Success", PortType.CONTROL, DataType.ANY,
                    false, false, "Success", PortPosition.RIGHT, Map.of()),
                new UIPort("failure", "Failure", PortType.CONTROL, DataType.ANY,
                    false, false, "Failure", PortPosition.RIGHT, Map.of())
            ),
            new UIConfiguration(
                List.of(
                    new ConfigField("executorType", "Executor Type", FieldType.SELECT,
                        null, true, null, "Type of executor", null,
                        null, List.of(), Map.of()),
                    new ConfigField("config", "Configuration", FieldType.JSON,
                        Map.of(), false, "{}", "Task configuration", null,
                        null, null, Map.of("language", "json")),
                    new ConfigField("timeout", "Timeout (seconds)", FieldType.NUMBER,
                        30, false, "30", "Execution timeout", null,
                        new FieldValidation(null, null, 1, 3600, null, null, null),
                        null, Map.of()),
                    new ConfigField("critical", "Critical", FieldType.CHECKBOX,
                        false, false, null, "Fail workflow if this fails", null,
                        null, null, Map.of())
                ),
                List.of(
                    new ConfigSection("basic", "Basic", "settings", false, true,
                        List.of("executorType", "config")),
                    new ConfigSection("advanced", "Advanced", "sliders", true, false,
                        List.of("timeout", "critical"))
                ),
                Map.of()
            ),
            new UIValidation(
                List.of(
                    new ValidationRule("executorType", ValidationType.REQUIRED,
                        "Executor type is required", Map.of())
                ),
                null
            )
        );
    }
}

/**
 * Decision Node Schema
 */
class DecisionNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema(
            "DECISION",
            new UIMetadata(
                "Decision",
                "Control Flow",
                "git-branch",
                "#F59E0B",
                "#FFFBEB",
                "#F59E0B",
                140,
                60,
                "Conditional branching based on expressions",
                List.of("decision", "if", "condition", "branch"),
                false,
                null
            ),
            List.of(
                new UIPort("in", "In", PortType.CONTROL, DataType.ANY,
                    true, false, "Input", PortPosition.LEFT, Map.of())
            ),
            List.of(
                new UIPort("true", "True", PortType.CONTROL, DataType.ANY,
                    false, false, "Condition true", PortPosition.RIGHT, Map.of()),
                new UIPort("false", "False", PortType.CONTROL, DataType.ANY,
                    false, false, "Condition false", PortPosition.RIGHT, Map.of())
            ),
            new UIConfiguration(
                List.of(
                    new ConfigField("condition", "Condition", FieldType.EXPRESSION,
                        null, true, "amount > 1000", "Boolean expression", 
                        "Use JavaScript expression syntax",
                        null, null, Map.of("language", "javascript")),
                    new ConfigField("description", "Description", FieldType.TEXTAREA,
                        null, false, "Describe the decision logic", 
                        "Human-readable description", null, null, null, Map.of())
                ),
                List.of(),
                Map.of()
            ),
            new UIValidation(
                List.of(
                    new ValidationRule("condition", ValidationType.REQUIRED,
                        "Condition expression is required", Map.of())
                ),
                null
            )
        );
    }
}

/**
 * HTTP Request Node Schema (Already shown in enhancements)
 */
class HttpRequestNodeSchema {
    public static NodeUISchema create() {
        // Implementation from earlier example
        return new NodeUISchema("HTTP_REQUEST", 
            new UIMetadata("HTTP Request", "Integration", "globe", 
                "#10B981", "#F3F4F6", "#10B981", 180, 80,
                "Make HTTP/REST API calls",
                List.of("http", "rest", "api"), false, null),
            List.of(new UIPort("in", "In", PortType.CONTROL, DataType.ANY, 
                true, false, "Trigger", PortPosition.LEFT, Map.of())),
            List.of(
                new UIPort("success", "Success", PortType.CONTROL, DataType.ANY,
                    false, false, "Success", PortPosition.RIGHT, Map.of()),
                new UIPort("failure", "Failure", PortType.CONTROL, DataType.ANY,
                    false, false, "Failure", PortPosition.RIGHT, Map.of())
            ),
            new UIConfiguration(List.of(), List.of(), Map.of()),
            new UIValidation(List.of(), null)
        );
    }
}

/**
 * LLM Call Node Schema
 */
class LLMCallNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema(
            "LLM_CALL",
            new UIMetadata(
                "LLM Call",
                "AI/ML",
                "sparkles",
                "#EC4899",
                "#FDF2F8",
                "#EC4899",
                200,
                100,
                "Call Large Language Model",
                List.of("ai", "llm", "gpt", "claude", "openai", "anthropic"),
                false,
                null
            ),
            List.of(
                new UIPort("in", "In", PortType.CONTROL, DataType.ANY,
                    true, false, "Trigger", PortPosition.LEFT, Map.of())
            ),
            List.of(
                new UIPort("success", "Success", PortType.CONTROL, DataType.ANY,
                    false, false, "Success", PortPosition.RIGHT, Map.of()),
                new UIPort("response", "Response", PortType.DATA, DataType.STRING,
                    false, false, "LLM response", PortPosition.RIGHT, Map.of())
            ),
            new UIConfiguration(
                List.of(
                    new ConfigField("provider", "Provider", FieldType.SELECT,
                        "openai", true, null, "LLM provider", null, null,
                        List.of(
                            new SelectOption("openai", "OpenAI", "openai", "GPT models"),
                            new SelectOption("anthropic", "Anthropic", "anthropic", "Claude models"),
                            new SelectOption("azure", "Azure OpenAI", "cloud", null),
                            new SelectOption("bedrock", "AWS Bedrock", "aws", null)
                        ),
                        Map.of()),
                    new ConfigField("model", "Model", FieldType.SELECT,
                        "gpt-4", true, null, "Model to use", null, null,
                        List.of(
                            new SelectOption("gpt-4", "GPT-4", null, null),
                            new SelectOption("gpt-3.5-turbo", "GPT-3.5 Turbo", null, null),
                            new SelectOption("claude-3-opus", "Claude 3 Opus", null, null),
                            new SelectOption("claude-3-sonnet", "Claude 3 Sonnet", null, null)
                        ),
                        Map.of()),
                    new ConfigField("systemPrompt", "System Prompt", FieldType.TEXTAREA,
                        null, false, "You are a helpful assistant", 
                        "System/instruction prompt", null, null, null, Map.of("rows", 3)),
                    new ConfigField("userPrompt", "User Prompt", FieldType.TEXTAREA,
                        null, true, "Enter prompt or use ${variable}", 
                        "User prompt (supports variables)", null, null, null,
                        Map.of("rows", 5, "expressions", true)),
                    new ConfigField("temperature", "Temperature", FieldType.SLIDER,
                        0.7, false, null, "Randomness (0-1)", null,
                        new FieldValidation(null, null, 0, 1, null, null, null),
                        null, Map.of("min", 0.0, "max", 1.0, "step", 0.1)),
                    new ConfigField("maxTokens", "Max Tokens", FieldType.NUMBER,
                        1000, false, "1000", "Maximum tokens to generate", null,
                        new FieldValidation(null, null, 1, 4096, null, null, null),
                        null, Map.of()),
                    new ConfigField("credentials", "API Key", FieldType.CREDENTIAL_SELECTOR,
                        null, true, null, "API credentials", null, null, null, Map.of())
                ),
                List.of(
                    new ConfigSection("model", "Model Configuration", "cpu", false, true,
                        List.of("provider", "model", "credentials")),
                    new ConfigSection("prompts", "Prompts", "message-square", false, true,
                        List.of("systemPrompt", "userPrompt")),
                    new ConfigSection("parameters", "Parameters", "sliders", true, false,
                        List.of("temperature", "maxTokens"))
                ),
                Map.of()
            ),
            new UIValidation(
                List.of(
                    new ValidationRule("userPrompt", ValidationType.REQUIRED,
                        "User prompt is required", Map.of())
                ),
                null
            )
        );
    }
}

/**
 * Database Query Node Schema
 */
class DatabaseQueryNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema(
            "DATABASE_QUERY",
            new UIMetadata(
                "Database Query",
                "Data",
                "database",
                "#6366F1",
                "#EEF2FF",
                "#6366F1",
                180,
                90,
                "Execute database query",
                List.of("database", "sql", "query", "data"),
                false,
                null
            ),
            List.of(
                new UIPort("in", "In", PortType.CONTROL, DataType.ANY,
                    true, false, "Trigger", PortPosition.LEFT, Map.of())
            ),
            List.of(
                new UIPort("success", "Success", PortType.CONTROL, DataType.ANY,
                    false, false, "Success", PortPosition.RIGHT, Map.of()),
                new UIPort("results", "Results", PortType.DATA, DataType.ARRAY,
                    false, false, "Query results", PortPosition.RIGHT, Map.of())
            ),
            new UIConfiguration(
                List.of(
                    new ConfigField("connectionString", "Connection", FieldType.CREDENTIAL_SELECTOR,
                        null, true, null, "Database connection", null, null, null, Map.of()),
                    new ConfigField("queryType", "Query Type", FieldType.SELECT,
                        "SELECT", true, null, "SQL operation type", null, null,
                        List.of(
                            new SelectOption("SELECT", "SELECT", null, "Retrieve data"),
                            new SelectOption("INSERT", "INSERT", null, "Insert data"),
                            new SelectOption("UPDATE", "UPDATE", null, "Update data"),
                            new SelectOption("DELETE", "DELETE", null, "Delete data")
                        ),
                        Map.of()),
                    new ConfigField("query", "SQL Query", FieldType.CODE,
                        null, true, "SELECT * FROM users WHERE id = ${userId}",
                        "SQL query (supports parameters)", null, null, null,
                        Map.of("language", "sql", "height", 150)),
                    new ConfigField("parameters", "Parameters", FieldType.KEY_VALUE,
                        Map.of(), false, null, "Query parameters", null, null, null,
                        Map.of("valueExpressions", true))
                ),
                List.of(),
                Map.of()
            ),
            new UIValidation(
                List.of(
                    new ValidationRule("query", ValidationType.REQUIRED,
                        "SQL query is required", Map.of())
                ),
                null
            )
        );
    }
}

// Additional schemas follow similar pattern...
// (Timer, Email, Kafka, etc.)

/**
 * Stub implementations for other node types
 */
class ParallelNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema("PARALLEL", 
            new UIMetadata("Parallel", "Control Flow", "git-merge", 
                "#8B5CF6", "#F5F3FF", "#8B5CF6", 140, 60,
                "Execute multiple branches in parallel",
                List.of("parallel", "fork", "concurrent"), false, null),
            List.of(), List.of(), new UIConfiguration(List.of(), List.of(), Map.of()),
            new UIValidation(List.of(), null));
    }
}

class AggregateNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema("AGGREGATE",
            new UIMetadata("Aggregate", "Control Flow", "layers",
                "#8B5CF6", "#F5F3FF", "#8B5CF6", 140, 60,
                "Wait for multiple branches to complete",
                List.of("aggregate", "join", "merge"), false, null),
            List.of(), List.of(), new UIConfiguration(List.of(), List.of(), Map.of()),
            new UIValidation(List.of(), null));
    }
}

class HumanTaskNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema("HUMAN_TASK",
            new UIMetadata("Human Task", "Human", "user",
                "#F59E0B", "#FFFBEB", "#F59E0B", 180, 80,
                "Task requiring human input",
                List.of("human", "manual", "approval"), false, null),
            List.of(), List.of(), new UIConfiguration(List.of(), List.of(), Map.of()),
            new UIValidation(List.of(), null));
    }
}

// Stubs for remaining node types
class ApprovalNodeSchema { public static NodeUISchema create() { return null; } }
class RestAPINodeSchema { public static NodeUISchema create() { return null; } }
class GraphQLNodeSchema { public static NodeUISchema create() { return null; } }
class GrpcCallNodeSchema { public static NodeUISchema create() { return null; } }
class DataTransformNodeSchema { public static NodeUISchema create() { return null; } }
class JsonPathNodeSchema { public static NodeUISchema create() { return null; } }
class KafkaProducerNodeSchema { public static NodeUISchema create() { return null; } }
class KafkaConsumerNodeSchema { public static NodeUISchema create() { return null; } }
class RabbitMQNodeSchema { public static NodeUISchema create() { return null; } }
class AwsLambdaNodeSchema { public static NodeUISchema create() { return null; } }
class S3OperationNodeSchema { public static NodeUISchema create() { return null; } }
class GcpStorageNodeSchema { public static NodeUISchema create() { return null; } }
class EmbeddingNodeSchema { public static NodeUISchema create() { return null; } }
class VectorSearchNodeSchema { public static NodeUISchema create() { return null; } }
class TimerNodeSchema { public static NodeUISchema create() { return null; } }
class EventWaitNodeSchema { public static NodeUISchema create() { return null; } }
class ScriptNodeSchema { public static NodeUISchema create() { return null; } }
class EmailNodeSchema { public static NodeUISchema create() { return null; } }

package tech.kayys.silat.ui.schemas;

import tech.kayys.silat.ui.*;
import java.util.*;

/**
 * ============================================================================
 * COMPLETE BUILT-IN NODE UI SCHEMAS
 * ============================================================================
 * 
 * All 25+ built-in node type schemas with complete configurations.
 * Each schema is production-ready with:
 * - Comprehensive field definitions
 * - Proper validation rules
 * - Conditional field visibility
 * - User-friendly descriptions
 * - Organized into logical sections
 * 
 * @author Silat Team
 */

// ==================== CONTROL FLOW NODES ====================

/**
 * Parallel Fork Node - Execute multiple branches concurrently
 */
class ParallelNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema(
            "PARALLEL",
            new UIMetadata(
                "Parallel Fork",
                "Control Flow",
                "git-merge",
                "#8B5CF6",
                "#F5F3FF",
                "#8B5CF6",
                140,
                80,
                "Execute multiple branches in parallel",
                List.of("parallel", "fork", "concurrent", "split"),
                false,
                null
            ),
            List.of(
                new UIPort("in", "In", PortType.CONTROL, DataType.ANY,
                    true, false, "Trigger parallel execution", PortPosition.LEFT, Map.of())
            ),
            List.of(
                new UIPort("branch-1", "Branch 1", PortType.CONTROL, DataType.ANY,
                    false, false, "First parallel branch", PortPosition.RIGHT, Map.of()),
                new UIPort("branch-2", "Branch 2", PortType.CONTROL, DataType.ANY,
                    false, false, "Second parallel branch", PortPosition.RIGHT, Map.of()),
                new UIPort("branch-3", "Branch 3", PortType.CONTROL, DataType.ANY,
                    false, false, "Third parallel branch", PortPosition.RIGHT, 
                    Map.of("dynamic", true))
            ),
            new UIConfiguration(
                List.of(
                    new ConfigField("branchCount", "Number of Branches", FieldType.NUMBER,
                        2, true, "2", "How many parallel branches", null,
                        new FieldValidation(null, null, 2, 10, null, null, null),
                        null, Map.of("step", 1)),
                    new ConfigField("waitStrategy", "Wait Strategy", FieldType.SELECT,
                        "ALL", true, null, "When to proceed", null, null,
                        List.of(
                            new SelectOption("ALL", "Wait for All", "check-circle",
                                "Wait for all branches to complete"),
                            new SelectOption("ANY", "Wait for Any", "zap",
                                "Proceed when first branch completes"),
                            new SelectOption("MAJORITY", "Wait for Majority", "users",
                                "Wait for majority to complete")
                        ),
                        Map.of()),
                    new ConfigField("failFast", "Fail Fast", FieldType.CHECKBOX,
                        true, false, null, "Cancel remaining branches on first failure",
                        null, null, null, Map.of()),
                    new ConfigField("timeout", "Overall Timeout (seconds)", FieldType.NUMBER,
                        300, false, "300", "Maximum time for all branches", null,
                        new FieldValidation(null, null, 1, 3600, null, null, null),
                        null, Map.of())
                ),
                List.of(
                    new ConfigSection("basic", "Configuration", "settings", false, true,
                        List.of("branchCount", "waitStrategy")),
                    new ConfigSection("advanced", "Advanced Options", "sliders", true, false,
                        List.of("failFast", "timeout"))
                ),
                Map.of()
            ),
            new UIValidation(
                List.of(
                    new ValidationRule("branchCount", ValidationType.MIN_VALUE,
                        "Must have at least 2 branches", Map.of("min", 2))
                ),
                null
            )
        );
    }
}

/**
 * Aggregate/Join Node - Wait for multiple branches
 */
class AggregateNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema(
            "AGGREGATE",
            new UIMetadata(
                "Aggregate",
                "Control Flow",
                "layers",
                "#8B5CF6",
                "#F5F3FF",
                "#8B5CF6",
                140,
                80,
                "Wait for and combine results from multiple branches",
                List.of("aggregate", "join", "merge", "combine"),
                false,
                null
            ),
            List.of(
                new UIPort("in-1", "Input 1", PortType.CONTROL, DataType.ANY,
                    true, false, "First input", PortPosition.LEFT, Map.of()),
                new UIPort("in-2", "Input 2", PortType.CONTROL, DataType.ANY,
                    true, false, "Second input", PortPosition.LEFT, Map.of()),
                new UIPort("in-3", "Input 3", PortType.CONTROL, DataType.ANY,
                    false, false, "Third input", PortPosition.LEFT, 
                    Map.of("dynamic", true))
            ),
            List.of(
                new UIPort("out", "Out", PortType.CONTROL, DataType.ANY,
                    false, false, "Continue after aggregation", PortPosition.RIGHT, Map.of()),
                new UIPort("results", "Results", PortType.DATA, DataType.ARRAY,
                    false, false, "Aggregated results", PortPosition.RIGHT, Map.of())
            ),
            new UIConfiguration(
                List.of(
                    new ConfigField("aggregationStrategy", "Strategy", FieldType.SELECT,
                        "MERGE", true, null, "How to aggregate results", null, null,
                        List.of(
                            new SelectOption("MERGE", "Merge Objects", "git-merge",
                                "Merge all results into single object"),
                            new SelectOption("ARRAY", "Array", "list",
                                "Collect results into array"),
                            new SelectOption("CUSTOM", "Custom Function", "code",
                                "Custom aggregation logic")
                        ),
                        Map.of()),
                    new ConfigField("customFunction", "Custom Function", FieldType.CODE,
                        null, false, "// results is array of inputs\nreturn results;",
                        "JavaScript aggregation function", null, null, null,
                        Map.of("language", "javascript", "height", 150)),
                    new ConfigField("timeout", "Timeout (seconds)", FieldType.NUMBER,
                        300, false, "300", "Max wait time for all inputs", null,
                        new FieldValidation(null, null, 1, 3600, null, null, null),
                        null, Map.of())
                ),
                List.of(),
                Map.of(
                    "customFunction", new ConditionalVisibility(
                        "aggregationStrategy", null, List.of("CUSTOM"))
                )
            ),
            new UIValidation(List.of(), null)
        );
    }
}

// ==================== HUMAN INTERACTION NODES ====================

/**
 * Human Task Node - Requires human intervention
 */
class HumanTaskNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema(
            "HUMAN_TASK",
            new UIMetadata(
                "Human Task",
                "Human",
                "user-check",
                "#F59E0B",
                "#FFFBEB",
                "#F59E0B",
                180,
                90,
                "Task requiring human input or action",
                List.of("human", "manual", "user", "input"),
                false,
                null
            ),
            List.of(
                new UIPort("in", "In", PortType.CONTROL, DataType.ANY,
                    true, false, "Trigger", PortPosition.LEFT, Map.of())
            ),
            List.of(
                new UIPort("completed", "Completed", PortType.CONTROL, DataType.ANY,
                    false, false, "Task completed", PortPosition.RIGHT, Map.of()),
                new UIPort("cancelled", "Cancelled", PortType.CONTROL, DataType.ANY,
                    false, false, "Task cancelled", PortPosition.RIGHT, Map.of()),
                new UIPort("response", "Response", PortType.DATA, DataType.OBJECT,
                    false, false, "Human response data", PortPosition.RIGHT, Map.of())
            ),
            new UIConfiguration(
                List.of(
                    new ConfigField("title", "Task Title", FieldType.TEXT,
                        null, true, "Review and approve document", "Task title", null,
                        new FieldValidation(1, 200, null, null, null, null, null),
                        null, Map.of()),
                    new ConfigField("description", "Description", FieldType.TEXTAREA,
                        null, true, "Please review the attached document and approve or reject",
                        "Detailed task description", null, null, null, Map.of("rows", 4)),
                    new ConfigField("assignee", "Assignee", FieldType.TEXT,
                        null, false, "user@example.com", "User or email to assign", 
                        "Leave empty to assign to role", null, null, Map.of()),
                    new ConfigField("assigneeRole", "Assignee Role", FieldType.SELECT,
                        null, false, null, "Role to assign task to", null, null,
                        List.of(
                            new SelectOption("manager", "Manager", "briefcase", null),
                            new SelectOption("admin", "Administrator", "shield", null),
                            new SelectOption("reviewer", "Reviewer", "eye", null),
                            new SelectOption("approver", "Approver", "check-circle", null)
                        ),
                        Map.of()),
                    new ConfigField("priority", "Priority", FieldType.SELECT,
                        "MEDIUM", false, null, "Task priority", null, null,
                        List.of(
                            new SelectOption("LOW", "Low", "arrow-down", null),
                            new SelectOption("MEDIUM", "Medium", "minus", null),
                            new SelectOption("HIGH", "High", "arrow-up", null),
                            new SelectOption("URGENT", "Urgent", "alert-triangle", null)
                        ),
                        Map.of()),
                    new ConfigField("dueDate", "Due Date", FieldType.DURATION,
                        "3d", false, "3d", "Time until due (e.g., 2h, 3d, 1w)", null,
                        null, null, Map.of()),
                    new ConfigField("formFields", "Form Fields", FieldType.JSON,
                        null, false, "[]", "Custom form fields for user input", null,
                        null, null, Map.of("language", "json", "height", 200)),
                    new ConfigField("notificationChannels", "Notifications", FieldType.MULTISELECT,
                        List.of("EMAIL"), false, null, "How to notify assignee", null, null,
                        List.of(
                            new SelectOption("EMAIL", "Email", "mail", null),
                            new SelectOption("SLACK", "Slack", "message-square", null),
                            new SelectOption("SMS", "SMS", "smartphone", null),
                            new SelectOption("IN_APP", "In-App", "bell", null)
                        ),
                        Map.of()),
                    new ConfigField("escalationEnabled", "Enable Escalation", FieldType.CHECKBOX,
                        false, false, null, "Escalate if not completed in time", null,
                        null, null, Map.of()),
                    new ConfigField("escalationTime", "Escalation Time", FieldType.DURATION,
                        "1d", false, "1d", "Time before escalation", null, null, null, Map.of()),
                    new ConfigField("escalateTo", "Escalate To", FieldType.TEXT,
                        null, false, "manager@example.com", "Who to escalate to", null,
                        null, null, Map.of())
                ),
                List.of(
                    new ConfigSection("task", "Task Details", "file-text", false, true,
                        List.of("title", "description", "priority", "dueDate")),
                    new ConfigSection("assignment", "Assignment", "user", false, true,
                        List.of("assignee", "assigneeRole", "notificationChannels")),
                    new ConfigSection("form", "Form Configuration", "layout", true, false,
                        List.of("formFields")),
                    new ConfigSection("escalation", "Escalation", "alert-circle", true, false,
                        List.of("escalationEnabled", "escalationTime", "escalateTo"))
                ),
                Map.of(
                    "escalationTime", new ConditionalVisibility(
                        "escalationEnabled", "value === true", null),
                    "escalateTo", new ConditionalVisibility(
                        "escalationEnabled", "value === true", null)
                )
            ),
            new UIValidation(
                List.of(
                    new ValidationRule("title", ValidationType.REQUIRED,
                        "Task title is required", Map.of()),
                    new ValidationRule("description", ValidationType.REQUIRED,
                        "Task description is required", Map.of())
                ),
                null
            )
        );
    }
}

/**
 * Approval Node - Specialized human task for approvals
 */
class ApprovalNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema(
            "APPROVAL",
            new UIMetadata(
                "Approval",
                "Human",
                "user-check",
                "#F59E0B",
                "#FFFBEB",
                "#F59E0B",
                160,
                80,
                "Approval workflow with approve/reject",
                List.of("approval", "review", "authorize", "approve"),
                false,
                null
            ),
            List.of(
                new UIPort("in", "In", PortType.CONTROL, DataType.ANY,
                    true, false, "Trigger", PortPosition.LEFT, Map.of())
            ),
            List.of(
                new UIPort("approved", "Approved", PortType.CONTROL, DataType.ANY,
                    false, false, "Approval granted", PortPosition.RIGHT, Map.of()),
                new UIPort("rejected", "Rejected", PortType.CONTROL, DataType.ANY,
                    false, false, "Approval rejected", PortPosition.RIGHT, Map.of())
            ),
            new UIConfiguration(
                List.of(
                    new ConfigField("approvalType", "Approval Type", FieldType.SELECT,
                        "SINGLE", true, null, "Type of approval process", null, null,
                        List.of(
                            new SelectOption("SINGLE", "Single Approver", "user", 
                                "One person approves"),
                            new SelectOption("SEQUENTIAL", "Sequential", "list",
                                "Multiple approvers in sequence"),
                            new SelectOption("PARALLEL", "Parallel", "users",
                                "Multiple approvers vote"),
                            new SelectOption("UNANIMOUS", "Unanimous", "check-circle",
                                "All must approve")
                        ),
                        Map.of()),
                    new ConfigField("approvers", "Approvers", FieldType.TEXTAREA,
                        null, true, "user1@example.com\nuser2@example.com",
                        "Email addresses (one per line)", null, null, null, 
                        Map.of("rows", 3)),
                    new ConfigField("title", "Request Title", FieldType.TEXT,
                        null, true, "Approval Request", "Title of approval", null,
                        null, null, Map.of()),
                    new ConfigField("details", "Request Details", FieldType.TEXTAREA,
                        null, true, "Please approve this request", "Details", null,
                        null, null, Map.of("rows", 4)),
                    new ConfigField("requireComments", "Require Comments", FieldType.CHECKBOX,
                        true, false, null, "Approver must provide comments", null,
                        null, null, Map.of()),
                    new ConfigField("dueDate", "Due Date", FieldType.DURATION,
                        "2d", false, "2d", "Time to respond", null, null, null, Map.of())
                ),
                List.of(
                    new ConfigSection("approval", "Approval Configuration", "check-circle", 
                        false, true,
                        List.of("approvalType", "approvers", "requireComments", "dueDate")),
                    new ConfigSection("request", "Request Details", "file-text", false, true,
                        List.of("title", "details"))
                ),
                Map.of()
            ),
            new UIValidation(
                List.of(
                    new ValidationRule("approvers", ValidationType.REQUIRED,
                        "At least one approver is required", Map.of())
                ),
                null
            )
        );
    }
}

// ==================== INTEGRATION NODES ====================

/**
 * REST API Node - Make REST API calls
 */
class RestAPINodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema(
            "REST_API",
            new UIMetadata(
                "REST API",
                "Integration",
                "globe",
                "#10B981",
                "#ECFDF5",
                "#10B981",
                180,
                90,
                "Call REST API endpoints",
                List.of("rest", "api", "http", "endpoint"),
                false,
                null
            ),
            List.of(
                new UIPort("in", "In", PortType.CONTROL, DataType.ANY,
                    true, false, "Trigger", PortPosition.LEFT, Map.of())
            ),
            List.of(
                new UIPort("success", "Success", PortType.CONTROL, DataType.ANY,
                    false, false, "2xx response", PortPosition.RIGHT, Map.of()),
                new UIPort("error", "Error", PortType.CONTROL, DataType.ANY,
                    false, false, "Error response", PortPosition.RIGHT, Map.of()),
                new UIPort("response", "Response", PortType.DATA, DataType.OBJECT,
                    false, false, "HTTP response", PortPosition.RIGHT, Map.of())
            ),
            new UIConfiguration(
                List.of(
                    new ConfigField("baseUrl", "Base URL", FieldType.TEXT,
                        null, false, "https://api.example.com", "API base URL", null,
                        new FieldValidation(null, null, null, null, "^https?://.*", null, null),
                        null, Map.of()),
                    new ConfigField("endpoint", "Endpoint", FieldType.TEXT,
                        null, true, "/users/${userId}", "API endpoint path", 
                        "Supports variable substitution", null, null, 
                        Map.of("expressions", true)),
                    new ConfigField("method", "HTTP Method", FieldType.SELECT,
                        "GET", true, null, "HTTP method", null, null,
                        List.of(
                            new SelectOption("GET", "GET", null, "Retrieve data"),
                            new SelectOption("POST", "POST", null, "Create resource"),
                            new SelectOption("PUT", "PUT", null, "Update resource"),
                            new SelectOption("PATCH", "PATCH", null, "Partial update"),
                            new SelectOption("DELETE", "DELETE", null, "Delete resource")
                        ),
                        Map.of()),
                    new ConfigField("headers", "Headers", FieldType.KEY_VALUE,
                        Map.of("Content-Type", "application/json"), false, null,
                        "HTTP headers", null, null, null, Map.of("valueExpressions", true)),
                    new ConfigField("queryParams", "Query Parameters", FieldType.KEY_VALUE,
                        Map.of(), false, null, "URL query parameters", null, null, null,
                        Map.of("valueExpressions", true)),
                    new ConfigField("body", "Request Body", FieldType.CODE,
                        null, false, "{}", "Request body (JSON)", null, null, null,
                        Map.of("language", "json", "height", 200)),
                    new ConfigField("authentication", "Authentication", FieldType.SELECT,
                        "NONE", false, null, "Auth method", null, null,
                        List.of(
                            new SelectOption("NONE", "None", null, null),
                            new SelectOption("BASIC", "Basic Auth", "lock", null),
                            new SelectOption("BEARER", "Bearer Token", "key", null),
                            new SelectOption("OAUTH2", "OAuth 2.0", "shield", null),
                            new SelectOption("API_KEY", "API Key", "hash", null)
                        ),
                        Map.of()),
                    new ConfigField("credentials", "Credentials", FieldType.CREDENTIAL_SELECTOR,
                        null, false, null, "Stored credentials", null, null, null, Map.of()),
                    new ConfigField("timeout", "Timeout (ms)", FieldType.NUMBER,
                        30000, false, "30000", "Request timeout", null,
                        new FieldValidation(null, null, 1000, 300000, null, null, null),
                        null, Map.of()),
                    new ConfigField("retries", "Max Retries", FieldType.NUMBER,
                        3, false, "3", "Retry attempts", null,
                        new FieldValidation(null, null, 0, 10, null, null, null),
                        null, Map.of()),
                    new ConfigField("followRedirects", "Follow Redirects", FieldType.CHECKBOX,
                        true, false, null, "Follow 3xx redirects", null, null, null, Map.of())
                ),
                List.of(
                    new ConfigSection("endpoint", "Endpoint", "link", false, true,
                        List.of("baseUrl", "endpoint", "method")),
                    new ConfigSection("request", "Request", "send", false, true,
                        List.of("headers", "queryParams", "body")),
                    new ConfigSection("auth", "Authentication", "lock", true, false,
                        List.of("authentication", "credentials")),
                    new ConfigSection("options", "Options", "settings", true, false,
                        List.of("timeout", "retries", "followRedirects"))
                ),
                Map.of(
                    "body", new ConditionalVisibility(
                        "method", null, List.of("POST", "PUT", "PATCH")),
                    "credentials", new ConditionalVisibility(
                        "authentication", null, 
                        List.of("BASIC", "BEARER", "OAUTH2", "API_KEY"))
                )
            ),
            new UIValidation(
                List.of(
                    new ValidationRule("endpoint", ValidationType.REQUIRED,
                        "Endpoint is required", Map.of())
                ),
                null
            )
        );
    }
}

/**
 * GraphQL Node - Execute GraphQL queries
 */
class GraphQLNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema(
            "GRAPHQL",
            new UIMetadata(
                "GraphQL",
                "Integration",
                "database",
                "#E11D48",
                "#FFF1F2",
                "#E11D48",
                180,
                90,
                "Execute GraphQL queries and mutations",
                List.of("graphql", "api", "query", "mutation"),
                false,
                null
            ),
            List.of(
                new UIPort("in", "In", PortType.CONTROL, DataType.ANY,
                    true, false, "Trigger", PortPosition.LEFT, Map.of())
            ),
            List.of(
                new UIPort("success", "Success", PortType.CONTROL, DataType.ANY,
                    false, false, "Query successful", PortPosition.RIGHT, Map.of()),
                new UIPort("data", "Data", PortType.DATA, DataType.OBJECT,
                    false, false, "Query result", PortPosition.RIGHT, Map.of())
            ),
            new UIConfiguration(
                List.of(
                    new ConfigField("endpoint", "GraphQL Endpoint", FieldType.TEXT,
                        null, true, "https://api.example.com/graphql", "GraphQL endpoint URL",
                        null, new FieldValidation(null, null, null, null, "^https?://.*", 
                        null, null), null, Map.of()),
                    new ConfigField("operationType", "Operation Type", FieldType.SELECT,
                        "QUERY", true, null, "GraphQL operation", null, null,
                        List.of(
                            new SelectOption("QUERY", "Query", "search", "Fetch data"),
                            new SelectOption("MUTATION", "Mutation", "edit", "Modify data"),
                            new SelectOption("SUBSCRIPTION", "Subscription", "rss", 
                                "Real-time updates")
                        ),
                        Map.of()),
                    new ConfigField("query", "GraphQL Query/Mutation", FieldType.CODE,
                        null, true, 
                        "query GetUser($id: ID!) {\n  user(id: $id) {\n    name\n    email\n  }\n}",
                        "GraphQL query or mutation", null, null, null,
                        Map.of("language", "graphql", "height", 250)),
                    new ConfigField("variables", "Variables", FieldType.CODE,
                        null, false, "{}", "GraphQL variables (JSON)", null, null, null,
                        Map.of("language", "json", "height", 150)),
                    new ConfigField("headers", "Headers", FieldType.KEY_VALUE,
                        Map.of(), false, null, "Additional headers", null, null, null,
                        Map.of()),
                    new ConfigField("authentication", "Authentication", FieldType.SELECT,
                        "NONE", false, null, "Auth method", null, null,
                        List.of(
                            new SelectOption("NONE", "None", null, null),
                            new SelectOption("BEARER", "Bearer Token", "key", null),
                            new SelectOption("API_KEY", "API Key", "hash", null)
                        ),
                        Map.of()),
                    new ConfigField("credentials", "Credentials", FieldType.CREDENTIAL_SELECTOR,
                        null, false, null, "API credentials", null, null, null, Map.of())
                ),
                List.of(
                    new ConfigSection("config", "Configuration", "settings", false, true,
                        List.of("endpoint", "operationType")),
                    new ConfigSection("operation", "Operation", "code", false, true,
                        List.of("query", "variables")),
                    new ConfigSection("auth", "Authentication", "lock", true, false,
                        List.of("headers", "authentication", "credentials"))
                ),
                Map.of(
                    "credentials", new ConditionalVisibility(
                        "authentication", null, List.of("BEARER", "API_KEY"))
                )
            ),
            new UIValidation(
                List.of(
                    new ValidationRule("endpoint", ValidationType.REQUIRED,
                        "GraphQL endpoint is required", Map.of()),
                    new ValidationRule("query", ValidationType.REQUIRED,
                        "GraphQL query/mutation is required", Map.of())
                ),
                null
            )
        );
    }
}

/**
 * gRPC Call Node - Make gRPC service calls
 */
class GrpcCallNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema(
            "GRPC_CALL",
            new UIMetadata(
                "gRPC Call",
                "Integration",
                "zap",
                "#7C3AED",
                "#F5F3FF",
                "#7C3AED",
                180,
                90,
                "Call gRPC service methods",
                List.of("grpc", "rpc", "protobuf", "service"),
                false,
                null
            ),
            List.of(
                new UIPort("in", "In", PortType.CONTROL, DataType.ANY,
                    true, false, "Trigger", PortPosition.LEFT, Map.of())
            ),
            List.of(
                new UIPort("success", "Success", PortType.CONTROL, DataType.ANY,
                    false, false, "RPC successful", PortPosition.RIGHT, Map.of()),
                new UIPort("response", "Response", PortType.DATA, DataType.OBJECT,
                    false, false, "RPC response", PortPosition.RIGHT, Map.of())
            ),
            new UIConfiguration(
                List.of(
                    new ConfigField("host", "gRPC Host", FieldType.TEXT,
                        null, true, "grpc.example.com:50051", "gRPC server host:port", null,
                        null, null, Map.of()),
                    new ConfigField("service", "Service Name", FieldType.TEXT,
                        null, true, "user.UserService", "Fully qualified service name", null,
                        null, null, Map.of()),
                    new ConfigField("method", "Method Name", FieldType.TEXT,
                        null, true, "GetUser", "RPC method to call", null,
                        null, null, Map.of()),
                    new ConfigField("request", "Request Message", FieldType.CODE,
                        null, true, "{}", "Request message (JSON)", null, null, null,
                        Map.of("language", "json", "height", 200)),
                    new ConfigField("metadata", "Metadata", FieldType.KEY_VALUE,
                        Map.of(), false, null, "gRPC metadata (headers)", null, null, null,
                        Map.of()),
                    new ConfigField("useTLS", "Use TLS", FieldType.CHECKBOX,
                        true, false, null, "Use secure connection", null, null, null, Map.of()),
                    new ConfigField("timeout", "Timeout (ms)", FieldType.NUMBER,
                        10000, false, "10000", "RPC timeout", null,
                        new FieldValidation(null, null, 1000, 60000, null, null, null),
                        null, Map.of())
                ),
                List.of(
                    new ConfigSection("service", "Service", "server", false, true,
                        List.of("host", "service", "method")),
                    new ConfigSection("request", "Request", "send", false, true,
                        List.of("request", "metadata")),
                    new ConfigSection("options", "Options", "settings", true, false,
                        List.of("useTLS", "timeout"))
                ),
                Map.of()
            ),
            new UIValidation(
                List.of(
                    new ValidationRule("host", ValidationType.REQUIRED,
                        "gRPC host is required", Map.of()),
                    new ValidationRule("service", ValidationType.REQUIRED,
                        "Service name is required", Map.of()),
                    new ValidationRule("method", ValidationType.REQUIRED,
                        "Method name is required", Map.of())
                ),
                null
            )
        );
    }
}

// ==================== DATA NODES ====================

/**
 * Data Transform Node - Transform data using JSONata/JMESPath
 */
class DataTransformNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema(
            "DATA_TRANSFORM",
            new UIMetadata(
                "Data Transform",
                "Data",
                "shuffle",
                "#0EA5E9",
                "#F0F9FF",
                "#0EA5E9",
                180,
                90,
                "Transform data using expressions",
                List.of("transform", "map", "convert", "jsonata"),
                false,
                null
            ),
            List.of(
                new UIPort("in", "In", PortType.CONTROL, DataType.ANY,
                    true, false, "Trigger", PortPosition.LEFT, Map.of()),
                new UIPort("data", "Data", PortType.DATA, DataType.OBJECT,
                    true, false, "Input data", PortPosition.LEFT, Map.of())
            ),
            List.of(
                new UIPort("out", "Out", PortType.CONTROL, DataType.ANY,
                    false, false, "Continue", PortPosition.RIGHT, Map.of()),
                new UIPort("result", "Result", PortType.DATA, DataType.OBJECT,
                    false, false, "Transformed data", PortPosition.RIGHT, Map.of())
            ),
            new UIConfiguration(
                List.of(
                    new ConfigField("transformLanguage", "Language", FieldType.SELECT,
                        "JSONATA", true, null, "Transformation language", null, null,
                        List.of(
                            new SelectOption("JSONATA", "JSONata", "code", "JSON query and transformation"),
                            new SelectOption("JMESPATH", "JMESPath", "code", "JSON query language"),
                            new SelectOption("JAVASCRIPT", "JavaScript", "code", "Custom JavaScript"),
                            new SelectOption("JOLT", "JOLT", "code", "JSON to JSON transformation")
                        ),
                        Map.of()),
                    new ConfigField("expression", "Expression", FieldType.CODE,
                        null, true, "$.user.name", "Transformation expression", null,
                        null, null, Map.of("language", "jsonata", "height", 250)),
                    new ConfigField("sampleInput", "Sample Input", FieldType.CODE,
                        null, false, "{}", "Test data for preview", null, null, null,
                        Map.of("language", "json", "height", 150)),
                    new ConfigField("validateOutput", "Validate Output", FieldType.CHECKBOX,
                        false, false, null, "Validate transformed data", null, null, null, Map.of()),
                    new ConfigField("outputSchema", "Output Schema", FieldType.CODE,
                        null, false, "{}", "JSON Schema for validation", null, null, null,
                        Map.of("language", "json", "height", 150))
                ),
                List.of(
                    new ConfigSection("transform", "Transformation", "shuffle", false, true,
                        List.of("transformLanguage", "expression")),
                    new ConfigSection("testing", "Testing", "play", true, false,
                        List.of("sampleInput")),
                    new ConfigSection("validation", "Validation", "check-circle", true, false,
                        List.of("validateOutput", "outputSchema"))
                ),
                Map.of(
                    "outputSchema", new ConditionalVisibility(
                        "validateOutput", "value === true", null)
                )
            ),
            new UIValidation(
                List.of(
                    new ValidationRule("expression", ValidationType.REQUIRED,
                        "Transformation expression is required", Map.of())
                ),
                null
            )
        );
    }
}

/**
 * JSON Path Node - Query JSON data
 */
class JsonPathNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema(
            "JSON_PATH",
            new UIMetadata(
                "JSON Path",
                "Data",
                "search",
                "#0EA5E9",
                "#F0F9FF",
                "#0EA5E9",
                160,
                80,
                "Query JSON data using JSONPath",
                List.of("json", "path", "query", "extract"),
                false,
                null
            ),
            List.of(
                new UIPort("in", "In", PortType.CONTROL, DataType.ANY,
                    true, false, "Trigger", PortPosition.LEFT, Map.of())
            ),
            List.of(
                new UIPort("out", "Out", PortType.CONTROL, DataType.ANY,
                    false, false, "Continue", PortPosition.RIGHT, Map.of()),
                new UIPort("result", "Result", PortType.DATA, DataType.ANY,
                    false, false, "Query result", PortPosition.RIGHT, Map.of())
            ),
            new UIConfiguration(
                List.of(
                    new ConfigField("jsonPath", "JSON Path", FieldType.TEXT,
                        null, true, "$.users[*].email", "JSONPath expression",
                        "Example: $.users[?(@.age > 18)].name", null, null, Map.of()),
                    new ConfigField("dataSource", "Data Source", FieldType.SELECT,
                        "CONTEXT", true, null, "Where to get JSON data", null, null,
                        List.of(
                            new SelectOption("CONTEXT", "Workflow Context", "database", null),
                            new SelectOption("INPUT", "Input Port", "arrow-right", null),
                            new SelectOption("INLINE", "Inline JSON", "code", null)
                        ),
                        Map.of()),
                    new ConfigField("contextPath", "Context Path", FieldType.TEXT,
                        null, false, "apiResponse", "Path in context", null, null, null, Map.of()),
                    new ConfigField("inlineData", "Inline JSON", FieldType.CODE,
                        null, false, "{}", "JSON data", null, null, null,
                        Map.of("language", "json", "height", 200))
                ),
                List.of(),
                Map.of(
                    "contextPath", new ConditionalVisibility(
                        "dataSource", null, List.of("CONTEXT")),
                    "inlineData", new ConditionalVisibility(
                        "dataSource", null, List.of("INLINE"))
                )
            ),
            new UIValidation(
                List.of(
                    new ValidationRule("jsonPath", ValidationType.REQUIRED,
                        "JSON Path expression is required", Map.of())
                ),
                null
            )
        );
    }
}

// ==================== MESSAGING NODES ====================

/**
 * Kafka Producer Node
 */
class KafkaProducerNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema(
            "KAFKA_PRODUCER",
            new UIMetadata(
                "Kafka Producer",
                "Messaging",
                "send",
                "#000000",
                "#F3F4F6",
                "#000000",
                180,
                90,
                "Publish messages to Kafka topic",
                List.of("kafka", "publish", "producer", "message", "event"),
                false,
                null
            ),
            List.of(
                new UIPort("in", "In", PortType.CONTROL, DataType.ANY,
                    true, false, "Trigger", PortPosition.LEFT, Map.of())
            ),
            List.of(
                new UIPort("success", "Success", PortType.CONTROL, DataType.ANY,
                    false, false, "Message sent", PortPosition.RIGHT, Map.of())
            ),
            new UIConfiguration(
                List.of(
                    new ConfigField("bootstrapServers", "Bootstrap Servers", FieldType.TEXT,
                        null, true, "localhost:9092", "Kafka broker addresses",
                        "Comma-separated list", null, null, Map.of()),
                    new ConfigField("topic", "Topic", FieldType.TEXT,
                        null, true, "events", "Kafka topic name", null, null, null, Map.of()),
                    new ConfigField("messageKey", "Message Key", FieldType.TEXT,
                        null, false, "${orderId}", "Message key (for partitioning)",
                        "Supports expressions", null, null, Map.of("expressions", true)),
                    new ConfigField("messageValue", "Message Value", FieldType.CODE,
                        null, true, "{}", "Message payload (JSON)", null, null, null,
                        Map.of("language", "json", "height", 200)),
                    new ConfigField("headers", "Headers", FieldType.KEY_VALUE,
                        Map.of(), false, null, "Message headers", null, null, null, Map.of()),
                    new ConfigField("partition", "Partition", FieldType.NUMBER,
                        null, false, "0", "Target partition (optional)", null,
                        new FieldValidation(null, null, 0, null, null, null, null),
                        null, Map.of()),
                    new ConfigField("compression", "Compression", FieldType.SELECT,
                        "NONE", false, null, "Message compression", null, null,
                        List.of(
                            new SelectOption("NONE", "None", null, null),
                            new SelectOption("GZIP", "GZIP", null, null),
                            new SelectOption("SNAPPY", "Snappy", null, null),
                            new SelectOption("LZ4", "LZ4", null, null),
                            new SelectOption("ZSTD", "Zstandard", null, null)
                        ),
                        Map.of()),
                    new ConfigField("acks", "Acknowledgments", FieldType.SELECT,
                        "ALL", false, null, "Required acknowledgments", null, null,
                        List.of(
                            new SelectOption("0", "None (Fire and Forget)", null, null),
                            new SelectOption("1", "Leader Only", null, null),
                            new SelectOption("ALL", "All In-Sync Replicas", null, null)
                        ),
                        Map.of())
                ),
                List.of(
                    new ConfigSection("connection", "Connection", "server", false, true,
                        List.of("bootstrapServers", "topic")),
                    new ConfigSection("message", "Message", "mail", false, true,
                        List.of("messageKey", "messageValue", "headers")),
                    new ConfigSection("options", "Options", "settings", true, false,
                        List.of("partition", "compression", "acks"))
                ),
                Map.of()
            ),
            new UIValidation(
                List.of(
                    new ValidationRule("bootstrapServers", ValidationType.REQUIRED,
                        "Bootstrap servers are required", Map.of()),
                    new ValidationRule("topic", ValidationType.REQUIRED,
                        "Topic is required", Map.of()),
                    new ValidationRule("messageValue", ValidationType.REQUIRED,
                        "Message value is required", Map.of())
                ),
                null
            )
        );
    }
}

/**
 * Kafka Consumer Node
 */
class KafkaConsumerNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema(
            "KAFKA_CONSUMER",
            new UIMetadata(
                "Kafka Consumer",
                "Messaging",
                "inbox",
                "#000000",
                "#F3F4F6",
                "#000000",
                180,
                90,
                "Consume messages from Kafka topic",
                List.of("kafka", "consumer", "subscribe", "message", "event"),
                false,
                null
            ),
            List.of(
                new UIPort("start", "Start", PortType.CONTROL, DataType.ANY,
                    true, false, "Start consuming", PortPosition.LEFT, Map.of())
            ),
            List.of(
                new UIPort("message", "Message", PortType.EVENT, DataType.OBJECT,
                    false, true, "For each message", PortPosition.RIGHT, Map.of()),
                new UIPort("data", "Data", PortType.DATA, DataType.OBJECT,
                    false, false, "Message data", PortPosition.RIGHT, Map.of())
            ),
            new UIConfiguration(
                List.of(
                    new ConfigField("bootstrapServers", "Bootstrap Servers", FieldType.TEXT,
                        null, true, "localhost:9092", "Kafka broker addresses", null,
                        null, null, Map.of()),
                    new ConfigField("topic", "Topic", FieldType.TEXT,
                        null, true, "events", "Kafka topic to subscribe", null,
                        null, null, Map.of()),
                    new ConfigField("groupId", "Consumer Group", FieldType.TEXT,
                        null, true, "workflow-consumers", "Consumer group ID", null,
                        null, null, Map.of()),
                    new ConfigField("autoOffsetReset", "Auto Offset Reset", FieldType.SELECT,
                        "LATEST", false, null, "Starting offset strategy", null, null,
                        List.of(
                            new SelectOption("EARLIEST", "Earliest", null, "From beginning"),
                            new SelectOption("LATEST", "Latest", null, "From end"),
                            new SelectOption("NONE", "None", null, "Fail if no offset")
                        ),
                        Map.of()),
                    new ConfigField("maxMessages", "Max Messages", FieldType.NUMBER,
                        100, false, "100", "Max messages to consume", null,
                        new FieldValidation(null, null, 1, 10000, null, null, null),
                        null, Map.of()),
                    new ConfigField("commitStrategy", "Commit Strategy", FieldType.SELECT,
                        "AUTO", false, null, "Offset commit strategy", null, null,
                        List.of(
                            new SelectOption("AUTO", "Auto Commit", null, null),
                            new SelectOption("MANUAL", "Manual Commit", null, null),
                            new SelectOption("SYNC", "Synchronous", null, null)
                        ),
                        Map.of())
                ),
                List.of(
                    new ConfigSection("connection", "Connection", "server", false, true,
                        List.of("bootstrapServers", "topic", "groupId")),
                    new ConfigSection("options", "Options", "settings", true, false,
                        List.of("autoOffsetReset", "maxMessages", "commitStrategy"))
                ),
                Map.of()
            ),
            new UIValidation(
                List.of(
                    new ValidationRule("bootstrapServers", ValidationType.REQUIRED,
                        "Bootstrap servers are required", Map.of()),
                    new ValidationRule("topic", ValidationType.REQUIRED,
                        "Topic is required", Map.of()),
                    new ValidationRule("groupId", ValidationType.REQUIRED,
                        "Consumer group is required", Map.of())
                ),
                null
            )
        );
    }
}

/**
 * RabbitMQ Node
 */
class RabbitMQNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema(
            "RABBITMQ",
            new UIMetadata(
                "RabbitMQ",
                "Messaging",
                "message-circle",
                "#FF6600",
                "#FFF7ED",
                "#FF6600",
                180,
                90,
                "Publish/consume RabbitMQ messages",
                List.of("rabbitmq", "amqp", "queue", "message"),
                false,
                null
            ),
            List.of(
                new UIPort("in", "In", PortType.CONTROL, DataType.ANY,
                    true, false, "Trigger", PortPosition.LEFT, Map.of())
            ),
            List.of(
                new UIPort("success", "Success", PortType.CONTROL, DataType.ANY,
                    false, false, "Operation successful", PortPosition.RIGHT, Map.of())
            ),
            new UIConfiguration(
                List.of(
                    new ConfigField("operation", "Operation", FieldType.SELECT,
                        "PUBLISH", true, null, "RabbitMQ operation", null, null,
                        List.of(
                            new SelectOption("PUBLISH", "Publish", "send", null),
                            new SelectOption("CONSUME", "Consume", "inbox", null)
                        ),
                        Map.of()),
                    new ConfigField("host", "Host", FieldType.TEXT,
                        "localhost", true, "localhost", "RabbitMQ host", null,
                        null, null, Map.of()),
                    new ConfigField("port", "Port", FieldType.NUMBER,
                        5672, false, "5672", "RabbitMQ port", null,
                        new FieldValidation(null, null, 1, 65535, null, null, null),
                        null, Map.of()),
                    new ConfigField("exchange", "Exchange", FieldType.TEXT,
                        null, false, "events", "Exchange name", null,
                        null, null, Map.of()),
                    new ConfigField("routingKey", "Routing Key", FieldType.TEXT,
                        null, false, "order.created", "Routing key", null,
                        null, null, Map.of()),
                    new ConfigField("queue", "Queue", FieldType.TEXT,
                        null, false, "order-queue", "Queue name", null,
                        null, null, Map.of()),
                    new ConfigField("message", "Message", FieldType.CODE,
                        null, false, "{}", "Message payload", null, null, null,
                        Map.of("language", "json", "height", 200)),
                    new ConfigField("credentials", "Credentials", FieldType.CREDENTIAL_SELECTOR,
                        null, false, null, "RabbitMQ credentials", null, null, null, Map.of())
                ),
                List.of(
                    new ConfigSection("connection", "Connection", "server", false, true,
                        List.of("host", "port", "credentials")),
                    new ConfigSection("routing", "Routing", "map", false, true,
                        List.of("operation", "exchange", "routingKey", "queue")),
                    new ConfigSection("message", "Message", "mail", true, false,
                        List.of("message"))
                ),
                Map.of(
                    "message", new ConditionalVisibility(
                        "operation", null, List.of("PUBLISH"))
                )
            ),
            new UIValidation(
                List.of(
                    new ValidationRule("host", ValidationType.REQUIRED,
                        "Host is required", Map.of())
                ),
                null
            )
        );
    }
}

// ==================== CLOUD PROVIDER NODES ====================

/**
 * AWS Lambda Node
 */
class AwsLambdaNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema(
            "AWS_LAMBDA",
            new UIMetadata(
                "AWS Lambda",
                "Cloud",
                "cloud",
                "#FF9900",
                "#FFF7ED",
                "#FF9900",
                180,
                90,
                "Invoke AWS Lambda function",
                List.of("aws", "lambda", "serverless", "function"),
                false,
                null
            ),
            List.of(
                new UIPort("in", "In", PortType.CONTROL, DataType.ANY,
                    true, false, "Trigger", PortPosition.LEFT, Map.of())
            ),
            List.of(
                new UIPort("success", "Success", PortType.CONTROL, DataType.ANY,
                    false, false, "Lambda successful", PortPosition.RIGHT, Map.of()),
                new UIPort("response", "Response", PortType.DATA, DataType.OBJECT,
                    false, false, "Lambda response", PortPosition.RIGHT, Map.of())
            ),
            new UIConfiguration(
                List.of(
                    new ConfigField("functionName", "Function Name", FieldType.TEXT,
                        null, true, "my-lambda-function", "Lambda function name or ARN", null,
                        null, null, Map.of()),
                    new ConfigField("region", "AWS Region", FieldType.SELECT,
                        "us-east-1", true, null, "AWS region", null, null,
                        List.of(
                            new SelectOption("us-east-1", "US East (N. Virginia)", null, null),
                            new SelectOption("us-west-2", "US West (Oregon)", null, null),
                            new SelectOption("eu-west-1", "EU (Ireland)", null, null),
                            new SelectOption("ap-southeast-1", "Asia Pacific (Singapore)", null, null)
                        ),
                        Map.of()),
                    new ConfigField("payload", "Payload", FieldType.CODE,
                        null, false, "{}", "Lambda event payload (JSON)", null,
                        null, null, Map.of("language", "json", "height", 200)),
                    new ConfigField("invocationType", "Invocation Type", FieldType.SELECT,
                        "RequestResponse", false, null, "How to invoke", null, null,
                        List.of(
                            new SelectOption("RequestResponse", "Synchronous", null, 
                                "Wait for response"),
                            new SelectOption("Event", "Asynchronous", null, 
                                "Fire and forget"),
                            new SelectOption("DryRun", "Dry Run", null, "Validate only")
                        ),
                        Map.of()),
                    new ConfigField("credentials", "AWS Credentials", FieldType.CREDENTIAL_SELECTOR,
                        null, true, null, "AWS access credentials", null,
                        null, null, Map.of()),
                    new ConfigField("timeout", "Timeout (seconds)", FieldType.NUMBER,
                        30, false, "30", "Invocation timeout", null,
                        new FieldValidation(null, null, 1, 900, null, null, null),
                        null, Map.of())
                ),
                List.of(
                    new ConfigSection("function", "Function", "zap", false, true,
                        List.of("functionName", "region", "credentials")),
                    new ConfigSection("invocation", "Invocation", "play", false, true,
                        List.of("payload", "invocationType", "timeout"))
                ),
                Map.of()
            ),
            new UIValidation(
                List.of(
                    new ValidationRule("functionName", ValidationType.REQUIRED,
                        "Function name is required", Map.of()),
                    new ValidationRule("credentials", ValidationType.REQUIRED,
                        "AWS credentials are required", Map.of())
                ),
                null
            )
        );
    }
}

/**
 * S3 Operation Node
 */
class S3OperationNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema(
            "S3_OPERATION",
            new UIMetadata(
                "S3 Operation",
                "Cloud",
                "hard-drive",
                "#FF9900",
                "#FFF7ED",
                "#FF9900",
                180,
                90,
                "AWS S3 file operations",
                List.of("aws", "s3", "storage", "file", "bucket"),
                false,
                null
            ),
            List.of(
                new UIPort("in", "In", PortType.CONTROL, DataType.ANY,
                    true, false, "Trigger", PortPosition.LEFT, Map.of())
            ),
            List.of(
                new UIPort("success", "Success", PortType.CONTROL, DataType.ANY,
                    false, false, "Operation successful", PortPosition.RIGHT, Map.of()),
                new UIPort("data", "Data", PortType.DATA, DataType.ANY,
                    false, false, "Operation result", PortPosition.RIGHT, Map.of())
            ),
            new UIConfiguration(
                List.of(
                    new ConfigField("operation", "Operation", FieldType.SELECT,
                        "GET", true, null, "S3 operation", null, null,
                        List.of(
                            new SelectOption("GET", "Get Object", "download", "Download file"),
                            new SelectOption("PUT", "Put Object", "upload", "Upload file"),
                            new SelectOption("DELETE", "Delete Object", "trash", "Delete file"),
                            new SelectOption("LIST", "List Objects", "list", "List bucket contents")
                        ),
                        Map.of()),
                    new ConfigField("bucket", "Bucket", FieldType.TEXT,
                        null, true, "my-bucket", "S3 bucket name", null,
                        null, null, Map.of()),
                    new ConfigField("key", "Object Key", FieldType.TEXT,
                        null, false, "folder/file.txt", "S3 object key (path)", null,
                        null, null, Map.of()),
                    new ConfigField("region", "Region", FieldType.SELECT,
                        "us-east-1", true, null, "AWS region", null, null,
                        List.of(
                            new SelectOption("us-east-1", "US East", null, null),
                            new SelectOption("us-west-2", "US West", null, null),
                            new SelectOption("eu-west-1", "EU West", null, null)
                        ),
                        Map.of()),
                    new ConfigField("credentials", "Credentials", FieldType.CREDENTIAL_SELECTOR,
                        null, true, null, "AWS credentials", null, null, null, Map.of())
                ),
                List.of(
                    new ConfigSection("config", "Configuration", "settings", false, true,
                        List.of("operation", "bucket", "key", "region", "credentials"))
                ),
                Map.of()
            ),
            new UIValidation(
                List.of(
                    new ValidationRule("bucket", ValidationType.REQUIRED,
                        "Bucket is required", Map.of())
                ),
                null
            )
        );
    }
}

/**
 * GCP Storage Node
 */
class GcpStorageNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema(
            "GCP_STORAGE",
            new UIMetadata(
                "GCP Storage",
                "Cloud",
                "database",
                "#4285F4",
                "#EFF6FF",
                "#4285F4",
                180,
                90,
                "Google Cloud Storage operations",
                List.of("gcp", "google", "storage", "bucket", "file"),
                false,
                null
            ),
            List.of(
                new UIPort("in", "In", PortType.CONTROL, DataType.ANY,
                    true, false, "Trigger", PortPosition.LEFT, Map.of())
            ),
            List.of(
                new UIPort("success", "Success", PortType.CONTROL, DataType.ANY,
                    false, false, "Operation successful", PortPosition.RIGHT, Map.of())
            ),
            new UIConfiguration(
                List.of(
                    new ConfigField("operation", "Operation", FieldType.SELECT,
                        "READ", true, null, "Storage operation", null, null,
                        List.of(
                            new SelectOption("READ", "Read", "download", null),
                            new SelectOption("WRITE", "Write", "upload", null),
                            new SelectOption("DELETE", "Delete", "trash", null),
                            new SelectOption("LIST", "List", "list", null)
                        ),
                        Map.of()),
                    new ConfigField("bucket", "Bucket", FieldType.TEXT,
                        null, true, "my-gcs-bucket", "GCS bucket name", null,
                        null, null, Map.of()),
                    new ConfigField("objectName", "Object Name", FieldType.TEXT,
                        null, false, "path/to/file.txt", "Object path", null,
                        null, null, Map.of()),
                    new ConfigField("credentials", "Credentials", FieldType.CREDENTIAL_SELECTOR,
                        null, true, null, "GCP service account credentials", null,
                        null, null, Map.of())
                ),
                List.of(),
                Map.of()
            ),
            new UIValidation(
                List.of(
                    new ValidationRule("bucket", ValidationType.REQUIRED,
                        "Bucket is required", Map.of())
                ),
                null
            )
        );
    }
}

// ==================== AI/ML NODES ====================

/**
 * Embedding Node - Generate embeddings
 */
class EmbeddingNodeSchema {
    public static NodeUISchema create() {
        return new NodeUISchema(
            "EMBEDDING",
            new UIMetadata(
                "Generate Embeddings",
                "AI/ML",
                "layers",
                "#EC4899",
                "#FDF2F8",
                "#EC4899",
                200,
                90,
                "Generate vector embeddings from text",
                List.of("ai", "embedding", "vector", "nlp"),
                false,
                null
            ),
            List.of(
                new UIPort("in", "In", PortType.CONTROL, DataType.ANY,
                    true, false, "Trigger", PortPosition.LEFT, Map.of())
            ),
            List.of(
                new UIPort("success", "Success", PortType.CONTROL, DataType.ANY,
                    false, false, "Embeddings generated", PortPosition.RIGHT, Map.of()),
                new UIPort("embeddings", "Embeddings", PortType.DATA, DataType.ARRAY,
                    false, false, "Vector embeddings", PortPosition.RIGHT, Map.of())
            ),
            new UIConfiguration(
                List.of(
                    new ConfigField("provider", "Provider", FieldType.SELECT,
                        "openai", true, null, "Embedding provider", null, null,
                        List.of(
                            new SelectOption("openai", "OpenAI", "openai", null),
                            new SelectOption("cohere", "Cohere", "cohere", null),
                            new SelectOption("huggingface", "Hugging Face", "huggingface", null)
                        ),
                        Map.of()),
                    new ConfigField("model", "Model", FieldType.SELECT,
                        "text-embedding-ada-002", true, null, "Embedding model", null, null,
                        List.of(
                            new SelectOption("text-embedding-ada-002", "Ada-002", null, null),
                            new SelectOption("text-embedding-3-small", "Embedding-3-Small", null, null),
                            new SelectOption("text-embedding-3-large", "Embedding-3-Large", null, null)
                        ),
                        Map.of()),
                    new ConfigField("text", "Text", FieldType.TEXTAREA,
                        null, true, "Enter text or use ${variable}", "Text to embed",
                        "Supports expressions", null, null,
                        Map.of("rows", 5, "expressions", true)),
                    new ConfigField("credentials", "API Key", FieldType.CREDENTIAL_SELECTOR,
                        null, true, null, "API credentials", null, null, null, Map.of())
                ),
                List.of(
                    new ConfigSection("config", "Configuration", "settings", false, true,
                        List.of("provider", "model", "credentials")),
                    new ConfigSection("input", "Input", "message-square", false, true,
                        List.of("text"))
                ),
                Map.of()
            ),
            new UIValidation(
                List.of(
                    new ValidationRule("text", ValidationType.REQUIRED,

                    