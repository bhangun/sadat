package tech.kayys.wayang.workflow.engine;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import tech.kayys.wayang.schema.workflow.WorkflowDefinition;
import tech.kayys.wayang.sdk.util.WorkflowValidator.ValidationResult;
import tech.kayys.wayang.workflow.model.ExecutionContext; // Corrected import
import tech.kayys.wayang.workflow.service.PolicyEngine;
import tech.kayys.wayang.workflow.service.ProvenanceService;
import tech.kayys.wayang.workflow.service.StateStore;
import tech.kayys.wayang.workflow.service.TelemetryService;
import tech.kayys.wayang.workflow.service.WorkflowExecutionStrategy;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.workflow.api.model.RunStatus;
import tech.kayys.wayang.workflow.exception.WorkflowValidationException;
import tech.kayys.wayang.workflow.service.WorkflowRegistry;
import tech.kayys.wayang.workflow.service.WorkflowValidator;
import tech.kayys.wayang.sdk.dto.TriggerWorkflowRequest;
import org.jboss.logging.Logger;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.enterprise.inject.Instance;

/**
 * WorkflowEngine - The sovereign orchestrator for workflow execution.
 */
@ApplicationScoped
public class WorkflowEngine {

    private static final Logger LOG = Logger.getLogger(WorkflowEngine.class);

    @Inject
    WorkflowRunManager runManager;

    @Inject
    StateStore stateStore;

    @Inject
    ProvenanceService provenanceService;

    @Inject
    TelemetryService telemetryService;

    @Inject
    PolicyEngine policyEngine;

    @Inject
    WorkflowValidator workflowValidator;

    @Inject
    WorkflowRegistry registry;

    @Inject
    Instance<WorkflowExecutionStrategy> executionStrategies;

    // Active execution monitors
    private final Map<String, ExecutionMonitor> activeRuns = new ConcurrentHashMap<>();

    private void verifyTenantAccess(String tenantId) {
        if (tech.kayys.wayang.workflow.security.context.SecurityContextHolder.hasContext()) {
            String authenticatedTenant = tech.kayys.wayang.workflow.security.context.SecurityContextHolder
                    .getCurrentTenantId();
            if (!authenticatedTenant.equals(tenantId)) {
                throw new SecurityException("Access denied: Tenant mismatch");
            }
        }
        // If no context (e.g. strict internal call without context), we might log a
        // warning or enforce policy.
        // For defense in depth, we assume context should be present for all external
        // triggers.
    }

    /**
     * Trigger a workflow execution based on a request.
     * This is the primary entry point for external triggers (API, Scheduler,
     * Events).
     */
    public Uni<TriggerWorkflowResponse> triggerWorkflow(TriggerWorkflowRequest request) {
        LOG.infof("Triggering workflow: %s (version: %s)", request.workflowId(), request.workflowVersion());

        Uni<WorkflowDefinition> workflowUni;
        if (request.workflowVersion() != null && !request.workflowVersion().isBlank()) {
            workflowUni = registry.getWorkflowByVersion(request.workflowId(), request.workflowVersion());
        } else {
            workflowUni = registry.getLatestVersion(request.workflowId());
        }

        return workflowUni
                .onItem().ifNull()
                .failWith(() -> new IllegalArgumentException("Workflow not found: " + request.workflowId()))
                .flatMap(workflow -> {
                    String tenantId = tech.kayys.wayang.workflow.security.context.SecurityContextHolder
                            .getCurrentTenantId();
                    return start(workflow, request.inputs(), tenantId);
                })
                .map(run -> new TriggerWorkflowResponse(run.getRunId(), run.getStatus().toString()));
    }

    public record TriggerWorkflowResponse(String runId, String status) {
    }

    /**
     * Start a new workflow execution with complete validation and setup.
     */
    public Uni<WorkflowRun> start(
            WorkflowDefinition workflow,
            Map<String, Object> inputs,
            String tenantId) {

        verifyTenantAccess(tenantId);
        LOG.infof("Starting workflow: %s (version: %s) for tenant: %s",
                workflow.getId().getValue(), workflow.getVersion(), tenantId);

        return workflowValidator.validate(workflow)
                .onItem().transformToUni(validationResult -> {
                    if (!validationResult.isValid()) {
                        LOG.errorf("Workflow validation failed for %s: %s", workflow.getId(),
                                validationResult.getErrors());
                        return Uni.createFrom().failure(
                                new WorkflowValidationException(
                                        "Workflow validation failed: " + validationResult.getErrors()));
                    }

                    ValidationResult inputValidation = validateWorkflowInputs(workflow, inputs);
                    if (!inputValidation.isValid()) {
                        LOG.errorf("Input validation failed for workflow %s: %s", workflow.getId(),
                                inputValidation.getMessage());
                        return Uni.createFrom().failure(
                                new WorkflowValidationException(
                                        "Input validation failed: " + inputValidation.getMessage()));
                    }

                    return policyEngine.validateWorkflowStart(workflow, tenantId);
                })
                .onItem().transformToUni(policyResult -> {
                    if (!policyResult.isAllowed()) {
                        LOG.warnf("Workflow start blocked by policy for tenant %s: %s", tenantId,
                                policyResult.getReason());
                        WorkflowRun blockedRun = createInitialRun(workflow, inputs, tenantId, RunStatus.BLOCKED);
                        blockedRun.setErrorMessage("Policy violation: " + policyResult.getReason());
                        return stateStore.save(blockedRun);
                    }

                    WorkflowRun run = createInitialRun(workflow, inputs, tenantId, RunStatus.PENDING);
                    return stateStore.save(run)
                            .onItem().call(savedRun -> provenanceService.logWorkflowStart(savedRun, workflow))
                            .onItem().invoke(telemetryService::recordWorkflowStart)
                            .onItem().transformToUni(savedRun -> executeWorkflow(savedRun, workflow));
                })
                .onFailure().recoverWithUni(th -> {
                    LOG.errorf(th, "Error during workflow start: %s", workflow.getId());
                    WorkflowRun failedRun = createInitialRun(workflow, inputs, tenantId, RunStatus.FAILED);
                    failedRun.setErrorMessage(th.getMessage());
                    failedRun.setStartedAt(Instant.now());
                    failedRun.setCompletedAt(Instant.now());
                    return stateStore.save(failedRun);
                });
    }

    private WorkflowRun createInitialRun(WorkflowDefinition workflow, Map<String, Object> inputs, String tenantId,
            RunStatus status) {
        return WorkflowRun.builder()
                .runId(UUID.randomUUID().toString())
                .workflowId(workflow.getId().getValue())
                .workflowVersion(workflow.getVersion())
                .tenantId(tenantId)
                .status(status)
                .inputs(inputs != null ? new HashMap<>(inputs) : new HashMap<>())
                .outputs(new HashMap<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * Main workflow execution logic.
     */
    private Uni<WorkflowRun> executeWorkflow(WorkflowRun run, WorkflowDefinition workflow) {
        LOG.infof("Executing workflow run: %s", run.getRunId());

        ExecutionMonitor monitor = new ExecutionMonitor(run.getRunId());
        activeRuns.put(run.getRunId(), monitor);

        run.setStatus(RunStatus.RUNNING);
        run.setStartedAt(Instant.now());
        run.setUpdatedAt(Instant.now());

        return stateStore.save(run)
                .onItem().transformToUni(savedRun -> {
                    ExecutionContext context = ExecutionContext.create(savedRun, workflow);
                    context.setWorkflowRun(savedRun);
                    context.setTenantId(run.getTenantId());
                    context.setCancellationChecker(() -> monitor.isCancelled());

                    WorkflowExecutionStrategy strategy = selectExecutionStrategy(workflow);
                    if (strategy == null) {
                        LOG.errorf("No suitable execution strategy found for workflow: %s", workflow.getId());
                        return completeWorkflow(savedRun, context, WorkflowCompletionStatus.FAILED,
                                "No execution strategy found");
                    }

                    LOG.debugf("Executing with strategy: %s", strategy.getStrategyType());

                    return strategy.execute(workflow, savedRun.getInputs(), context)
                            .onItem().transformToUni(executedRun -> handleExecutionResult(executedRun, workflow))
                            .onFailure().recoverWithUni(th -> {
                                LOG.errorf(th, "Execution failed for run: %s", run.getRunId());
                                return completeWorkflow(savedRun, context, WorkflowCompletionStatus.FAILED,
                                        th.getMessage());
                            });
                })
                .eventually(() -> activeRuns.remove(run.getRunId()));
    }

    private Uni<WorkflowRun> handleExecutionResult(WorkflowRun executedRun, WorkflowDefinition workflow) {
        ExecutionContext finalContext = ExecutionContext.create(executedRun, workflow);

        boolean hasFailures = executedRun.getNodeStates().values().stream()
                .anyMatch(ns -> ns.status() == tech.kayys.wayang.sdk.dto.NodeExecutionState.NodeStatus.FAILED);

        if (finalContext.isAwaitingHuman() || executedRun.getStatus() == RunStatus.SUSPENDED) {
            LOG.infof("Workflow run %s suspended for human intervention", executedRun.getRunId());
            return suspendForHITL(executedRun, finalContext);
        }

        WorkflowCompletionStatus status = hasFailures ? WorkflowCompletionStatus.FAILED
                : WorkflowCompletionStatus.COMPLETED;
        return completeWorkflow(executedRun, finalContext, status, null);
    }

    /**
     * Select the appropriate execution strategy for the given workflow
     */
    private WorkflowExecutionStrategy selectExecutionStrategy(WorkflowDefinition workflow) {
        // First, check if workflow explicitly specifies a strategy via metadata
        if (workflow.getMetadata() != null && workflow.getMetadata().containsKey("executionStrategy")) {
            String requestedStrategy = (String) workflow.getMetadata().get("executionStrategy");
            LOG.debugf("Workflow %s requests execution strategy: %s", workflow.getId(), requestedStrategy);

            for (WorkflowExecutionStrategy strategy : executionStrategies) {
                if (requestedStrategy.equals(strategy.getStrategyType())) {
                    LOG.infof("Selected %s strategy for workflow %s", requestedStrategy, workflow.getId());
                    return strategy;
                }
            }

            LOG.warnf("Requested strategy %s not found for workflow %s", requestedStrategy, workflow.getId());
        }

        // Second, let strategies determine if they can handle this workflow
        for (WorkflowExecutionStrategy strategy : executionStrategies) {
            if (strategy.canHandle(workflow)) {
                LOG.infof("Strategy %s can handle workflow %s", strategy.getStrategyType(), workflow.getId());
                return strategy;
            }
        }

        // Finally, use DAG strategy as default fallback
        for (WorkflowExecutionStrategy strategy : executionStrategies) {
            if ("DAG".equals(strategy.getStrategyType())) {
                LOG.debugf("Using default DAG strategy for workflow %s", workflow.getId());
                return strategy;
            }
        }

        // If no strategy matches, return null
        LOG.errorf("No execution strategy found for workflow: %s", workflow.getId());
        return null;
    }

    private Uni<WorkflowRun> completeWorkflow(WorkflowRun run, ExecutionContext context,
            WorkflowCompletionStatus status, String msg) {

        LOG.infof("Completing workflow %s with status: %s", run.getRunId(), status);

        run.setStatus(status == WorkflowCompletionStatus.COMPLETED ? RunStatus.COMPLETED : RunStatus.FAILED);
        run.setCompletedAt(Instant.now());
        run.setUpdatedAt(Instant.now());

        if (msg != null) {
            run.setErrorMessage(msg);
        }

        // Try to capture final outputs from context if available
        if (context != null && context.getWorkflowRun() != null) {
            run.setOutputs(context.getWorkflowRun().getOutputs());
        }

        return stateStore.save(run)
                .onItem().call(savedRun -> {
                    if (status == WorkflowCompletionStatus.COMPLETED) {
                        return provenanceService.logWorkflowComplete(savedRun);
                    } else {
                        return provenanceService.logRunFailed(savedRun, savedRun.getErrorMessage());
                    }
                })
                .onItem().invoke(savedRun -> {
                    long duration = savedRun.getCompletedAt() != null && savedRun.getStartedAt() != null
                            ? java.time.Duration.between(savedRun.getStartedAt(), savedRun.getCompletedAt()).toMillis()
                            : 0;
                    telemetryService.recordWorkflowCompletion(savedRun, duration);
                });
    }

    private ValidationResult validateWorkflowInputs(WorkflowDefinition w, Map<String, Object> i) {
        // Placeholder for future schema-based input validation
        return ValidationResult.success();
    }

    private Uni<WorkflowRun> suspendForHITL(WorkflowRun run, ExecutionContext context) {
        LOG.infof("Suspending workflow %s for Human-in-the-Loop", run.getRunId());
        run.setStatus(RunStatus.SUSPENDED);
        run.setUpdatedAt(Instant.now());
        return stateStore.save(run);
    }

    public Uni<WorkflowRun> execute(String runId, String tenantId) {
        LOG.infof("Executing workflow run: %s", runId);

        return runManager.getRun(runId)
                .onItem().ifNull().failWith(() -> new IllegalArgumentException("Run not found: " + runId))
                .flatMap(run -> {
                    if (!run.getTenantId().equals(tenantId)) {
                        return Uni.createFrom().failure(new IllegalStateException("Tenant mismatch for run: " + runId));
                    }
                    verifyTenantAccess(tenantId);
                    return registry.getWorkflowByVersion(run.getWorkflowId(), run.getWorkflowVersion())
                            .onItem().ifNull()
                            .failWith(() -> new IllegalArgumentException(
                                    "Workflow definition not found for run: " + runId))
                            .flatMap(workflow -> executeWorkflow(run, workflow));
                });
    }

    public Uni<WorkflowRun> resume(String runId, String tenantId) {
        verifyTenantAccess(tenantId);
        LOG.infof("Resuming workflow run: %s", runId);
        return runManager.resumeRun(runId, tenantId, null, null)
                .flatMap(run -> registry.getWorkflowByVersion(run.getWorkflowId(), run.getWorkflowVersion())
                        .flatMap(workflow -> executeWorkflow(run, workflow)));
    }

    public Uni<Void> pause(String runId, String tenantId) {
        verifyTenantAccess(tenantId);
        LOG.infof("Pausing workflow run: %s", runId);
        return runManager.updateRunStatus(runId, tenantId, RunStatus.PAUSED);
    }

    public Uni<Void> cancel(String runId, String tenantId, String reason) {
        verifyTenantAccess(tenantId);
        LOG.infof("Canceling workflow run: %s (reason: %s)", runId, reason);
        ExecutionMonitor monitor = activeRuns.get(runId);
        if (monitor != null) {
            monitor.cancel();
        }
        return runManager.cancelRun(runId, tenantId, reason).replaceWithVoid();
    }

    @lombok.Getter
    private static class ExecutionMonitor {
        private final String runId;
        private volatile boolean cancelled = false;

        ExecutionMonitor(String runId) {
            this.runId = runId;
        }

        void cancel() {
            this.cancelled = true;
        }
    }

    private enum WorkflowCompletionStatus {
        COMPLETED, FAILED
    }
}
