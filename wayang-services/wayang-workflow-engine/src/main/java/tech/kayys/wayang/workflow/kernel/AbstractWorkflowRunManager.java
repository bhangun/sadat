package tech.kayys.wayang.workflow.kernel;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.workflow.kernel.AbstractWorkflowRunManager.WorkflowAction;
import tech.kayys.wayang.workflow.kernel.AbstractWorkflowRunManager.WorkflowDecision;
import tech.kayys.wayang.workflow.kernel.DistributedWorkflowRunManager.ClusterRunStats;
import tech.kayys.wayang.workflow.kernel.ManagedWorkflowComponent.ComponentMetrics;
import tech.kayys.wayang.workflow.kernel.ManagedWorkflowComponent.HealthStatus;
import tech.kayys.wayang.workflow.repository.WorkflowRunRepository;
import tech.kayys.wayang.workflow.security.context.SecurityContextHolder;
import tech.kayys.wayang.workflow.service.DistributedLockManager;
import tech.kayys.wayang.workflow.v1.WorkflowEvent;
import tech.kayys.wayang.sdk.util.WorkflowValidator.ValidationResult;
import tech.kayys.wayang.workflow.api.model.RunStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow.Subscription;

/**
 * Abstract base implementation for WorkflowRunManager
 */
@ApplicationScoped
public abstract class AbstractWorkflowRunManager
        extends TenantAwareComponent
        implements DistributedWorkflowRunManager {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractWorkflowRunManager.class);

    @Inject
    protected WorkflowRunRepository runRepository;

    @Inject
    protected DistributedLockManager lockManager;

    @Inject
    protected EventStore eventStore;

    @Inject
    protected ClusterCoordinator clusterCoordinator;

    protected final Map<String, List<RunStateChangeListener>> listeners = new ConcurrentHashMap<>();
    protected volatile HealthStatus healthStatus = HealthStatus.HEALTHY;
    protected volatile ComponentConfig config;

    @Override
    public Uni<WorkflowRunId> createRun(
            WorkflowDescriptor workflow,
            Map<String, Object> inputs,
            RunTrigger trigger) {

        return validateNotNull("workflow", workflow,
                validateNotNull("trigger", trigger,
                        Uni.createFrom().deferred(() -> {

                            String tenantId = SecurityContextHolder.getCurrentTenantId();
                            verifyTenantAccess(tenantId);

                            // Generate unique run ID
                            String runId = generateRunId(workflow, tenantId);
                            WorkflowRunId workflowRunId = new WorkflowRunId(runId);

                            LOG.info("Creating workflow run {} for workflow {} in tenant {}",
                                    runId, workflow.getId(), tenantId);

                            // Create initial run state
                            WorkflowRunState initialState = WorkflowRunState.builder()
                                    .runId(runId)
                                    .workflowId(workflow.getId())
                                    .tenantId(tenantId)
                                    .status(RunStatus.PENDING)
                                    .inputs(inputs != null ? new HashMap<>(inputs) : new HashMap<>())
                                    .trigger(trigger)
                                    .createdAt(Instant.now())
                                    .build();

                            // Store initial state
                            return runRepository.save(initialState)
                                    .onItem().transform(savedState -> {
                                        // Record creation event
                                        recordRunEvent(runId, "CREATED", Map.of(
                                                "workflowId", workflow.getId(),
                                                "tenantId", tenantId,
                                                "triggerType", trigger.getType()));

                                        // Notify listeners
                                        notifyRunCreated(workflowRunId, initialState);

                                        return workflowRunId;
                                    })
                                    .onFailure().recoverWithUni(th -> {
                                        LOG.error("Failed to create workflow run", th);
                                        return Uni.createFrom().failure(
                                                new RuntimeException(
                                                        "Failed to create workflow run: " + th.getMessage(), th));
                                    });
                        })));
    }

    @Override
    public Uni<Void> startRun(WorkflowRunId runId) {
        return validateNotNull("runId", runId,
                Uni.createFrom().deferred(() -> {

                    String tenantId = SecurityContextHolder.getCurrentTenantId();
                    verifyTenantAccess(tenantId);

                    return runRepository.findById(runId.value())
                            .onItem().ifNull()
                            .failWith(() -> new IllegalArgumentException("Run not found: " + runId.value()))
                            .flatMap(runState -> {
                                // Validate state transition
                                ValidationResult validation = WorkflowValidationUtils
                                        .validateStateTransition(runState.getStatus(), RunStatus.RUNNING);
                                if (!validation.isValid()) {
                                    return Uni.createFrom().failure(
                                            new IllegalStateException(validation.getMessage()));
                                }

                                // Update state
                                runState.setStatus(RunStatus.RUNNING);
                                runState.setStartedAt(Instant.now());

                                return runRepository.update(runState)
                                        .onItem().invoke(updatedState -> {
                                            recordRunEvent(runId.value(), "STARTED", Map.of(
                                                    "oldStatus", RunStatus.PENDING.name(),
                                                    "newStatus", RunStatus.RUNNING.name()));
                                            notifyStatusChanged(runId, RunStatus.RUNNING);
                                        })
                                        .replaceWithVoid();
                            });
                }));
    }

    @Override
    public Uni<Void> handleNodeResult(
            WorkflowRunId runId,
            NodeExecutionResult result) {

        return validateNotNull("runId", runId,
                validateNotNull("result", result,
                        Uni.createFrom().deferred(() -> {

                            String tenantId = SecurityContextHolder.getCurrentTenantId();
                            verifyTenantAccess(tenantId);

                            LOG.debug("Handling node result for run {}: node {} status {}",
                                    runId.value(), result.getNodeId(), result.getStatus());

                            return runRepository.findById(runId.value())
                                    .onItem().ifNull()
                                    .failWith(() -> new IllegalArgumentException("Run not found: " + runId.value()))
                                    .flatMap(runState -> {
                                        // Update node state in run
                                        runState.recordNodeExecution(
                                                result.getNodeId(),
                                                result.getStatus(),
                                                result.getOutputs(),
                                                result.getErrorMessage());

                                        // Check if workflow should continue, complete, or fail
                                        WorkflowDecision decision = makeWorkflowDecision(runState, result);

                                        switch (decision.getAction()) {
                                            case CONTINUE:
                                                // Nothing to do, workflow continues
                                                break;
                                            case COMPLETE:
                                                runState.setStatus(RunStatus.SUCCEEDED);
                                                runState.setCompletedAt(Instant.now());
                                                runState.setOutputs(decision.getOutputs());
                                                break;
                                            case FAIL:
                                                runState.setStatus(RunStatus.FAILED);
                                                runState.setCompletedAt(Instant.now());
                                                runState.setError(decision.getError());
                                                break;
                                            case SUSPEND:
                                                runState.setStatus(RunStatus.SUSPENDED);
                                                break;
                                        }

                                        return runRepository.update(runState)
                                                .onItem().invoke(updatedState -> {
                                                    recordRunEvent(runId.value(), "NODE_RESULT_HANDLED", Map.of(
                                                            "nodeId", result.getNodeId(),
                                                            "status", result.getStatus().name(),
                                                            "workflowAction", decision.getAction().name()));

                                                    notifyNodeExecuted(runId, result.getNodeId(), result);

                                                    if (decision.getAction() != WorkflowAction.CONTINUE) {
                                                        notifyStatusChanged(runId, updatedState.getStatus());
                                                    }
                                                })
                                                .replaceWithVoid();
                                    });
                        })));
    }

    @Override
    public Uni<Void> handleNodeResultWithOCC(
            WorkflowRunId runId,
            NodeExecutionResult result,
            long expectedVersion) {

        return validateNotNull("runId", runId,
                validateNotNull("result", result,
                        Uni.createFrom().deferred(() -> {

                            String tenantId = SecurityContextHolder.getCurrentTenantId();
                            verifyTenantAccess(tenantId);

                            return runRepository.findByIdWithVersion(runId.value())
                                    .onItem().ifNull()
                                    .failWith(() -> new IllegalArgumentException("Run not found: " + runId.value()))
                                    .flatMap(runState -> {
                                        // Optimistic concurrency check
                                        if (runState.getVersion() != expectedVersion) {
                                            return Uni.createFrom().failure(
                                                    new ConcurrentModificationException(
                                                            "Run state was modified by another process. Expected version: "
                                                                    +
                                                                    expectedVersion + ", actual: "
                                                                    + runState.getVersion()));
                                        }

                                        // Proceed with normal handling
                                        return handleNodeResult(runId, result);
                                    });
                        })));
    }

    @Override
    public Uni<Void> signal(WorkflowRunId runId, Signal signal) {
        return validateNotNull("runId", runId,
                validateNotNull("signal", signal,
                        Uni.createFrom().deferred(() -> {

                            String tenantId = SecurityContextHolder.getCurrentTenantId();
                            verifyTenantAccess(tenantId);

                            LOG.info("Processing signal for run {}: {}", runId.value(), signal.getType());

                            return runRepository.findById(runId.value())
                                    .onItem().ifNull()
                                    .failWith(() -> new IllegalArgumentException("Run not found: " + runId.value()))
                                    .flatMap(runState -> {
                                        // Validate run can receive signals
                                        if (!canReceiveSignal(runState.getStatus())) {
                                            return Uni.createFrom().failure(
                                                    new IllegalStateException(
                                                            "Run in state " + runState.getStatus()
                                                                    + " cannot receive signals"));
                                        }

                                        // Process signal based on type
                                        switch (signal.getType()) {
                                            case RESUME:
                                                if (runState.getStatus() != RunStatus.SUSPENDED &&
                                                        runState.getStatus() != RunStatus.PAUSED) {
                                                    return Uni.createFrom().failure(
                                                            new IllegalStateException(
                                                                    "Can only resume suspended or paused runs"));
                                                }
                                                runState.setStatus(RunStatus.RUNNING);
                                                break;
                                            case CANCEL:
                                                runState.setStatus(RunStatus.CANCELLED);
                                                runState.setCompletedAt(Instant.now());
                                                break;
                                            case PAUSE:
                                                runState.setStatus(RunStatus.PAUSED);
                                                break;
                                            case UPDATE:
                                                if (signal.getData() != null) {
                                                    runState.updateWorkflowState(signal.getData());
                                                }
                                                break;
                                        }

                                        return runRepository.update(runState)
                                                .onItem().invoke(updatedState -> {
                                                    recordRunEvent(runId.value(), "SIGNAL_PROCESSED", Map.of(
                                                            "signalType", signal.getType().name(),
                                                            "newStatus", updatedState.getStatus().name()));
                                                    notifyStatusChanged(runId, updatedState.getStatus());
                                                })
                                                .replaceWithVoid();
                                    });
                        })));
    }

    @Override
    public Uni<Void> cancelRun(WorkflowRunId runId, CancelReason reason) {
        return validateNotNull("runId", runId,
                validateNotNull("reason", reason,
                        Uni.createFrom().deferred(() -> {

                            String tenantId = SecurityContextHolder.getCurrentTenantId();
                            verifyTenantAccess(tenantId);

                            LOG.info("Cancelling run {}: {}", runId.value(), reason.getMessage());

                            return runRepository.findById(runId.value())
                                    .onItem().ifNull()
                                    .failWith(() -> new IllegalArgumentException("Run not found: " + runId.value()))
                                    .flatMap(runState -> {
                                        // Validate can cancel
                                        ValidationResult validation = WorkflowValidationUtils
                                                .validateStateTransition(runState.getStatus(), RunStatus.CANCELLED);
                                        if (!validation.isValid()) {
                                            return Uni.createFrom().failure(
                                                    new IllegalStateException(validation.getMessage()));
                                        }

                                        runState.setStatus(RunStatus.CANCELLED);
                                        runState.setCompletedAt(Instant.now());
                                        runState.setCancellationReason(reason.getMessage());

                                        // Execute compensation if needed
                                        return executeCompensation(runState)
                                                .flatMap(compensationResult -> runRepository.update(runState))
                                                .onItem().invoke(updatedState -> {
                                                    recordRunEvent(runId.value(), "CANCELLED", Map.of(
                                                            "reason", reason.getMessage(),
                                                            "compensationExecuted", compensationResult != null));
                                                    notifyStatusChanged(runId, RunStatus.CANCELLED);
                                                })
                                                .replaceWithVoid();
                                    });
                        })));
    }

    @Override
    public Uni<WorkflowRunId> createRunWithLock(
            WorkflowDescriptor workflow,
            Map<String, Object> inputs,
            RunTrigger trigger,
            DistributedLockConfig lockConfig) {

        String lockKey = "workflow:create:" + workflow.getId() + ":" +
                SecurityContextHolder.getCurrentTenantId();

        return lockManager.acquire(lockKey, lockConfig.timeout())
                .onItem().ifNull()
                .failWith(() -> new IllegalStateException("Could not acquire lock for workflow creation"))
                .flatMap(lock -> createRun(workflow, inputs, trigger)
                        .onItem().invoke(runId -> LOG.debug("Created run {} with lock", runId.value()))
                        .eventually(() -> lockManager.release(lock)
                                .onFailure().invoke(th -> LOG.warn("Failed to release lock {}", lockKey, th))));
    }

    @Override
    public Uni<Subscription> subscribeToRunChanges(
            WorkflowRunId runId,
            RunStateChangeListener listener) {

        return Uni.createFrom().deferred(() -> {
            listeners.computeIfAbsent(runId.value(), k -> new CopyOnWriteArrayList<>())
                    .add(listener);

            Subscription subscription = new Subscription() {
                @Override
                public void unsubscribe() {
                    List<RunStateChangeListener> runListeners = listeners.get(runId.value());
                    if (runListeners != null) {
                        runListeners.remove(listener);
                        if (runListeners.isEmpty()) {
                            listeners.remove(runId.value());
                        }
                    }
                }

                @Override
                public boolean isSubscribed() {
                    List<RunStateChangeListener> runListeners = listeners.get(runId.value());
                    return runListeners != null && runListeners.contains(listener);
                }
            };

            return Uni.createFrom().item(subscription);
        });
    }

    @Override
    public Uni<ClusterRunStats> getClusterRunStats(String tenantId) {
        return validateNotEmpty("tenantId", tenantId,
                Uni.createFrom().deferred(() -> {

                    verifyTenantAccess(tenantId);

                    return clusterCoordinator.getClusterInfo()
                            .flatMap(clusterInfo -> runRepository.getClusterStatistics(tenantId)
                                    .map(stats -> new ClusterRunStats(
                                            clusterInfo.getNodeCount(),
                                            clusterInfo.getActiveNodeCount(),
                                            stats.getRunsPerNode(),
                                            stats.getStatusDistribution())));
                }));
    }

    @Override
    public Uni<Void> replicateRunState(WorkflowRunId runId, String targetNodeId) {
        return validateNotNull("runId", runId,
                validateNotEmpty("targetNodeId", targetNodeId,
                        Uni.createFrom().deferred(() -> {

                            return runRepository.findById(runId.value())
                                    .flatMap(runState -> clusterCoordinator.replicateState(
                                            targetNodeId, "workflow-run:" + runId.value(), runState))
                                    .replaceWithVoid();
                        })));
    }

    @Override
    public Uni<Void> initialize(ComponentConfig config) {
        return Uni.createFrom().deferred(() -> {
            this.config = config;
            LOG.info("Initializing WorkflowRunManager with config: {}", config.componentName());
            return Uni.createFrom().voidItem();
        });
    }

    @Override
    public Uni<Void> start() {
        return Uni.createFrom().deferred(() -> {
            healthStatus = HealthStatus.HEALTHY;
            LOG.info("WorkflowRunManager started");
            return Uni.createFrom().voidItem();
        });
    }

    @Override
    public Uni<Void> stop() {
        return Uni.createFrom().deferred(() -> {
            healthStatus = HealthStatus.UNHEALTHY;
            listeners.clear();
            LOG.info("WorkflowRunManager stopped");
            return Uni.createFrom().voidItem();
        });
    }

    @Override
    public Uni<HealthStatus> healthCheck() {
        return Uni.createFrom().deferred(() -> {
            // Check repository health
            return runRepository.healthCheck()
                    .map(repoHealth -> {
                        if (repoHealth == HealthStatus.UNHEALTHY) {
                            return HealthStatus.UNHEALTHY;
                        }
                        return healthStatus;
                    });
        });
    }

    @Override
    public Uni<ComponentMetrics> getMetrics() {
        return Uni.createFrom().deferred(() -> {
            return runRepository.getMetrics()
                    .map(repoMetrics -> new ComponentMetrics(
                            repoMetrics.totalOperations(),
                            repoMetrics.successfulOperations(),
                            repoMetrics.failedOperations(),
                            repoMetrics.averageLatencyMs(),
                            Map.of(
                                    "activeListeners", listeners.values().stream()
                                            .mapToInt(List::size).sum(),
                                    "clusterEnabled", clusterCoordinator.isEnabled())));
        });
    }

    // Query methods from original interface
    @Override
    public Uni<WorkflowRunSnapshot> getSnapshot(WorkflowRunId runId) {
        return validateNotNull("runId", runId,
                Uni.createFrom().deferred(() -> {
                    return runRepository.findById(runId.value())
                            .map(WorkflowRunSnapshot::fromState);
                }));
    }

    @Override
    public Uni<ExecutionHistory> getExecutionHistory(WorkflowRunId runId) {
        return validateNotNull("runId", runId,
                Uni.createFrom().deferred(() -> {
                    return eventStore.getEvents(runId.value())
                            .map(events -> new ExecutionHistory(runId.value(), events));
                }));
    }

    @Override
    public Uni<ValidationResult> validateTransition(
            WorkflowRunId runId,
            WorkflowRunState targetState) {

        return validateNotNull("runId", runId,
                validateNotNull("targetState", targetState,
                        Uni.createFrom().deferred(() -> {
                            return runRepository.findById(runId.value())
                                    .map(currentState -> WorkflowValidationUtils
                                            .validateStateTransition(currentState.getStatus(),
                                                    targetState.getStatus()));
                        })));
    }

    // External integration methods
    @Override
    public Uni<Void> onNodeExecutionCompleted(
            NodeExecutionResult result,
            String executorSignature) {

        return validateNotNull("result", result,
                validateNotEmpty("executorSignature", executorSignature,
                        Uni.createFrom().deferred(() -> {

                            // Verify executor signature
                            if (!isValidExecutorSignature(executorSignature)) {
                                return Uni.createFrom().failure(
                                        new SecurityException("Invalid executor signature"));
                            }

                            WorkflowRunId runId = new WorkflowRunId(result.getRunId());
                            return handleNodeResult(runId, result);
                        })));
    }

    @Override
    public Uni<Void> onExternalSignal(
            WorkflowRunId runId,
            ExternalSignal signal,
            String callbackToken) {

        return validateNotNull("runId", runId,
                validateNotNull("signal", signal,
                        validateNotEmpty("callbackToken", callbackToken,
                                Uni.createFrom().deferred(() -> {

                                    // Verify callback token
                                    if (!isValidCallbackToken(runId, callbackToken)) {
                                        return Uni.createFrom().failure(
                                                new SecurityException("Invalid callback token"));
                                    }

                                    // Convert external signal to internal signal
                                    Signal internalSignal = convertExternalSignal(signal);
                                    return signal(runId, internalSignal);
                                }))));
    }

    @Override
    public Uni<CallbackRegistration> registerCallback(
            WorkflowRunId runId,
            String nodeId,
            CallbackConfig config) {

        return validateNotNull("runId", runId,
                validateNotEmpty("nodeId", nodeId,
                        validateNotNull("config", config,
                                Uni.createFrom().deferred(() -> {

                                    String callbackToken = generateCallbackToken(runId, nodeId);
                                    String callbackUrl = buildCallbackUrl(config, callbackToken);

                                    CallbackRegistration registration = new CallbackRegistration(
                                            callbackToken,
                                            callbackUrl,
                                            config.getTimeout(),
                                            Instant.now().plus(config.getTimeout()));

                                    // Store registration
                                    return callbackStore.save(registration)
                                            .replaceWith(registration);
                                }))));
    }

    @Override
    public Uni<ExecutionToken> createExecutionToken(
            WorkflowRunId runId,
            String nodeId,
            int attempt) {

        return validateNotNull("runId", runId,
                validateNotEmpty("nodeId", nodeId,
                        Uni.createFrom().deferred(() -> {

                            String token = generateExecutionToken(runId, nodeId, attempt);
                            Instant expiresAt = Instant.now().plus(Duration.ofHours(1));

                            ExecutionToken executionToken = new ExecutionToken(
                                    token,
                                    runId.value(),
                                    nodeId,
                                    attempt,
                                    expiresAt);

                            return tokenStore.save(executionToken)
                                    .replaceWith(executionToken);
                        })));
    }

    // Protected helper methods
    protected String generateRunId(WorkflowDescriptor workflow, String tenantId) {
        return String.format("%s-%s-%s",
                workflow.getId(),
                tenantId,
                UUID.randomUUID().toString().substring(0, 8));
    }

    protected void recordRunEvent(String runId, String eventType, Map<String, Object> data) {
        WorkflowEvent event = WorkflowEvent.builder()
                .runId(runId)
                .type(eventType)
                .timestamp(Instant.now())
                .data(data != null ? new HashMap<>(data) : Map.of())
                .build();

        eventStore.append(event).subscribe().with(
                success -> LOG.debug("Recorded event {} for run {}", eventType, runId),
                failure -> LOG.error("Failed to record event for run {}", runId, failure));
    }

    protected void notifyRunCreated(WorkflowRunId runId, WorkflowRunState state) {
        List<RunStateChangeListener> runListeners = listeners.get(runId.value());
        if (runListeners != null) {
            for (RunStateChangeListener listener : runListeners) {
                try {
                    listener.onStateChanged(runId, state);
                } catch (Exception e) {
                    LOG.error("Error notifying listener for run {}", runId.value(), e);
                }
            }
        }
    }

    protected void notifyStatusChanged(WorkflowRunId runId, RunStatus newStatus) {
        List<RunStateChangeListener> runListeners = listeners.get(runId.value());
        if (runListeners != null) {
            for (RunStateChangeListener listener : runListeners) {
                try {
                    listener.onStateChanged(runId,
                            WorkflowRunState.builder().status(newStatus).build());
                } catch (Exception e) {
                    LOG.error("Error notifying status change for run {}", runId.value(), e);
                }
            }
        }
    }

    protected void notifyNodeExecuted(WorkflowRunId runId, String nodeId,
            NodeExecutionResult result) {
        List<RunStateChangeListener> runListeners = listeners.get(runId.value());
        if (runListeners != null) {
            for (RunStateChangeListener listener : runListeners) {
                try {
                    listener.onNodeExecuted(runId, nodeId, result);
                } catch (Exception e) {
                    LOG.error("Error notifying node execution for run {}", runId.value(), e);
                }
            }
        }
    }

    protected WorkflowDecision makeWorkflowDecision(WorkflowRunState runState,
            NodeExecutionResult result) {
        // Implementation depends on workflow definition
        // This is a simplified version
        if (result.getStatus() == NodeExecutionStatus.FAILED) {
            return WorkflowDecision.fail("Node execution failed: " + result.getErrorMessage());
        }

        // Check if all nodes are completed
        if (allNodesCompleted(runState)) {
            return WorkflowDecision.complete(runState.getOutputs());
        }

        return WorkflowDecision.continueWorkflow();
    }

    protected boolean allNodesCompleted(WorkflowRunState runState) {
        // Implementation depends on workflow definition
        return false; // Simplified
    }

    protected boolean canReceiveSignal(RunStatus status) {
        return switch (status) {
            case RUNNING, SUSPENDED, PAUSED -> true;
            default -> false;
        };
    }

    protected Uni<CompensationResult> executeCompensation(WorkflowRunState runState) {
        // Implementation for compensation logic
        return Uni.createFrom().item(new CompensationResult(true, "Compensation executed"));
    }

    protected boolean isValidExecutorSignature(String signature) {
        // Implementation for signature validation
        return signature != null && !signature.isEmpty();
    }

    protected boolean isValidCallbackToken(WorkflowRunId runId, String token) {
        // Implementation for token validation
        return token != null && !token.isEmpty();
    }

    protected Signal convertExternalSignal(ExternalSignal externalSignal) {
        return Signal.builder()
                .type(SignalType.valueOf(externalSignal.getType().name()))
                .data(externalSignal.getData())
                .source("external")
                .build();
    }

    protected String generateCallbackToken(WorkflowRunId runId, String nodeId) {
        return String.format("callback-%s-%s-%s",
                runId.value(),
                nodeId,
                UUID.randomUUID().toString().substring(0, 8));
    }

    protected String buildCallbackUrl(CallbackConfig config, String token) {
        return String.format("%s/callback/%s", config.getBaseUrl(), token);
    }

    protected String generateExecutionToken(WorkflowRunId runId, String nodeId, int attempt) {
        return String.format("exec-%s-%s-%d-%s",
                runId.value(),
                nodeId,
                attempt,
                UUID.randomUUID().toString().substring(0, 8));
    }

    // Inner classes for decision making
    protected enum WorkflowAction {
        CONTINUE, COMPLETE, FAIL, SUSPEND
    }

    protected static class WorkflowDecision {
        private final WorkflowAction action;
        private final Map<String, Object> outputs;
        private final String error;

        private WorkflowDecision(WorkflowAction action, Map<String, Object> outputs, String error) {
            this.action = action;
            this.outputs = outputs;
            this.error = error;
        }

        public static WorkflowDecision continueWorkflow() {
            return new WorkflowDecision(WorkflowAction.CONTINUE, null, null);
        }

        public static WorkflowDecision complete(Map<String, Object> outputs) {
            return new WorkflowDecision(WorkflowAction.COMPLETE, outputs, null);
        }

        public static WorkflowDecision fail(String error) {
            return new WorkflowDecision(WorkflowAction.FAIL, null, error);
        }

        public static WorkflowDecision suspend() {
            return new WorkflowDecision(WorkflowAction.SUSPEND, null, null);
        }

        public WorkflowAction getAction() {
            return action;
        }

        public Map<String, Object> getOutputs() {
            return outputs;
        }

        public String getError() {
            return error;
        }
    }

    protected record CompensationResult(boolean success, String message) {
    }
}