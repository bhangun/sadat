package tech.kayys.wayang.workflow.kernel;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.sdk.util.WorkflowValidator.ValidationResult;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.workflow.repository.WorkflowRepository;
import tech.kayys.wayang.workflow.repository.WorkflowRunRepository;

/**
 * Default implementation of the state authority.
 */
@ApplicationScoped
public class DefaultWorkflowRunManager implements WorkflowRunManager {

    private static final Logger LOG = Logger.getLogger(DefaultWorkflowRunManager.class);

    @Inject
    WorkflowRepository workflowRepository;

    @Inject
    WorkflowRunRepository runRepository;

    @Inject
    EventPublisher eventPublisher;

    @Inject
    SchedulerService scheduler;

    @Inject
    RetryPolicyManager retryPolicyManager;

    @Inject
    WorkflowEngine workflowEngine;

    // In-memory tracking (can be distributed via Redis in production)
    private final Map<String, ActiveRunState> activeRuns = new ConcurrentHashMap<>();

    @Override
    public Uni<WorkflowRunId> createRun(
            WorkflowDescriptor workflow,
            ExecutionContext initialContext,
            RunTrigger trigger) {

        LOG.infof("Creating run for workflow: %s, tenant: %s",
                workflow.getId(), initialContext.getTenantId());

        // 1. Create run entity
        WorkflowRun run = WorkflowRun.create(
                workflow.getId(),
                workflow.getVersion(),
                initialContext,
                trigger);

        // 2. Save to persistent storage
        return runRepository.save(run)
                .onItem().invoke(savedRun -> {
                    // 3. Initialize active state
                    activeRuns.put(savedRun.getId(), new ActiveRunState(savedRun));

                    // 4. Emit creation event
                    eventPublisher.publish(new WorkflowRunCreated(savedRun.getId(), workflow, trigger));

                    LOG.infof("Run created: %s", savedRun.getId());
                })
                .map(WorkflowRun::getId);
    }

    @Override
    public Uni<Void> startRun(WorkflowRunId runId) {
        LOG.infof("Starting run: %s", runId);

        return getRun(runId)
                .flatMap(run -> {
                    // 1. Validate transition
                    validateStateTransition(run.getState(), WorkflowRunState.RUNNING);

                    // 2. Update state
                    run.setState(WorkflowRunState.RUNNING);
                    run.setStartedAt(Instant.now());

                    // 3. Persist
                    return runRepository.save(run)
                            .onItem().invoke(savedRun -> {
                                // 4. Update active state
                                activeRuns.get(runId).setRun(savedRun);

                                // 5. Emit event
                                eventPublisher.publish(new WorkflowRunStarted(runId));

                                // 6. Schedule first node
                                scheduleNextNode(savedRun);
                            });
                })
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> handleNodeResult(
            WorkflowRunId runId,
            NodeExecutionResult result) {

        LOG.infof("Handling node result for run: %s, node: %s, status: %s",
                runId, result.getNodeId(), result.getStatus());

        return getRun(runId)
                .flatMap(run -> {
                    ActiveRunState activeState = activeRuns.get(runId);
                    if (activeState == null) {
                        return Uni.createFrom().failure(
                                new IllegalStateException("Run not active: " + runId));
                    }

                    // Record execution in history
                    run.recordNodeExecution(result);

                    // Handle based on result status
                    return switch (result.getStatus()) {
                        case SUCCESS -> handleSuccess(run, activeState, result);
                        case FAILURE -> handleFailure(run, activeState, result);
                        case WAIT -> handleWait(run, activeState, result);
                    };
                });
    }

    @Override
    public Uni<Void> signal(WorkflowRunId runId, Signal signal) {
        LOG.infof("Signaling run: %s with signal: %s", runId, signal.getType());

        return getRun(runId)
                .flatMap(run -> {
                    if (run.getState() != WorkflowRunState.WAITING) {
                        return Uni.createFrom().failure(
                                new IllegalStateException("Run not in WAITING state: " + runId));
                    }

                    ActiveRunState activeState = activeRuns.get(runId);
                    if (activeState == null) {
                        return Uni.createFrom().failure(
                                new IllegalStateException("Run not active: " + runId));
                    }

                    // Update context with signal
                    ExecutionContext updatedContext = run.getExecutionContext()
                            .withVariable("signal", signal.getData(), "signal")
                            .withMetadata("signalReceivedAt", Instant.now().toString());

                    run.setExecutionContext(updatedContext);
                    run.setState(WorkflowRunState.RUNNING);

                    return runRepository.save(run)
                            .onItem().invoke(savedRun -> {
                                activeState.setRun(savedRun);
                                eventPublisher.publish(new WorkflowRunResumed(runId, signal));
                                scheduleNextNode(savedRun);
                            })
                            .replaceWithVoid();
                });
    }

    @Override
    public Uni<Void> cancelRun(WorkflowRunId runId, CancelReason reason) {
        LOG.infof("Cancelling run: %s, reason: %s", runId, reason);

        return getRun(runId)
                .flatMap(run -> {
                    validateStateTransition(run.getState(), WorkflowRunState.CANCELLED);

                    run.setState(WorkflowRunState.CANCELLED);
                    run.setCancelReason(reason.toString());
                    run.setCompletedAt(Instant.now());

                    return runRepository.save(run)
                            .onItem().invoke(savedRun -> {
                                // Cleanup
                                activeRuns.remove(runId);
                                scheduler.cancelAllForRun(runId);

                                // Emit event
                                eventPublisher.publish(new WorkflowRunCancelled(runId, reason));

                                // Initiate compensation if needed
                                if (reason.requiresCompensation()) {
                                    initiateCompensation(savedRun);
                                }
                            })
                            .replaceWithVoid();
                });
    }

    @Override
    public Uni<WorkflowRunSnapshot> getSnapshot(WorkflowRunId runId) {
        return getRun(runId)
                .map(run -> {
                    ActiveRunState activeState = activeRuns.get(runId);

                    return WorkflowRunSnapshot.builder()
                            .runId(runId)
                            .state(run.getState())
                            .context(run.getExecutionContext())
                            .nodeHistory(run.getNodeExecutionHistory())
                            .activeNode(activeState != null ? activeState.getCurrentNode() : null)
                            .waitInfo(activeState != null ? activeState.getWaitInfo() : null)
                            .retryInfo(activeState != null ? activeState.getRetryInfo() : Map.of())
                            .build();
                });
    }

    @Override
    public Uni<ExecutionHistory> getExecutionHistory(WorkflowRunId runId) {
        return runRepository.getExecutionHistory(runId);
    }

    @Override
    public Uni<ValidationResult> validateTransition(
            WorkflowRunId runId,
            WorkflowRunState targetState) {

        return getRun(runId)
                .map(run -> {
                    try {
                        validateStateTransition(run.getState(), targetState);
                        return ValidationResult.success();
                    } catch (IllegalStateException e) {
                        return ValidationResult.failure(e.getMessage());
                    }
                });
    }

    // ================ PRIVATE IMPLEMENTATION DETAILS ================

    private Uni<WorkflowRun> getRun(WorkflowRunId runId) {
        return runRepository.findById(runId)
                .onItem().ifNull().failWith(() -> new IllegalArgumentException("Run not found: " + runId));
    }

    private Uni<Void> handleSuccess(
            WorkflowRun run,
            ActiveRunState activeState,
            NodeExecutionResult result) {

        // Update context if provided
        if (result.getUpdatedContext() != null) {
            run.setExecutionContext(result.getUpdatedContext());
        }

        // Clear retry info
        activeState.clearRetryInfo(result.getNodeId());

        // Emit event
        eventPublisher.publish(new NodeExecutionSucceeded(
                run.getId(),
                result.getNodeId(),
                result.getDuration()));

        // Schedule next node or complete
        return determineNextNode(run)
                .flatMap(nextNode -> {
                    if (nextNode != null) {
                        return scheduleNodeExecution(run, nextNode);
                    } else {
                        return completeWorkflow(run);
                    }
                });
    }

    private Uni<Void> handleFailure(
            WorkflowRun run,
            ActiveRunState activeState,
            NodeExecutionResult result) {

        ExecutionError error = result.getError();
        activeState.recordFailure(result.getNodeId(), error);

        eventPublisher.publish(new NodeExecutionFailed(
                run.getId(),
                result.getNodeId(),
                error));

        // Check retry eligibility
        if (error.isRetriable() && activeState.canRetry(result.getNodeId())) {
            return scheduleRetry(run, activeState, result);
        } else {
            return failWorkflow(run, error);
        }
    }

    private Uni<Void> handleWait(
            WorkflowRun run,
            ActiveRunState activeState,
            NodeExecutionResult result) {

        run.setState(WorkflowRunState.WAITING);
        activeState.setWaitInfo(result.getWaitInfo());

        return runRepository.save(run)
                .onItem().invoke(savedRun -> {
                    activeState.setRun(savedRun);

                    eventPublisher.publish(new WorkflowRunWaiting(
                            run.getId(),
                            result.getNodeId(),
                            result.getWaitInfo()));

                    scheduleWaitTimeout(savedRun, result);
                })
                .replaceWithVoid();
    }

    private Uni<Void> scheduleRetry(
            WorkflowRun run,
            ActiveRunState activeState,
            NodeExecutionResult failedResult) {

        String nodeId = failedResult.getNodeId();
        int retryCount = activeState.getRetryCount(nodeId);

        run.setState(WorkflowRunState.RETRYING);

        return runRepository.save(run)
                .onItem().invoke(savedRun -> {
                    activeState.setRun(savedRun);

                    Duration delay = retryPolicyManager.calculateDelay(nodeId, retryCount);

                    eventPublisher.publish(new NodeRetryScheduled(
                            run.getId(),
                            nodeId,
                            retryCount + 1,
                            delay));

                    scheduler.schedule(
                            run.getId(),
                            "retry-" + nodeId,
                            Instant.now().plus(delay),
                            () -> executeNode(run, nodeId, retryCount + 1));
                })
                .replaceWithVoid();
    }

    private Uni<Void> completeWorkflow(WorkflowRun run) {
        run.setState(WorkflowRunState.COMPLETED);
        run.setCompletedAt(Instant.now());

        return runRepository.save(run)
                .onItem().invoke(savedRun -> {
                    activeRuns.remove(run.getId());
                    scheduler.cancelAllForRun(run.getId());

                    eventPublisher.publish(new WorkflowRunCompleted(
                            run.getId(),
                            savedRun.getExecutionContext()));
                })
                .replaceWithVoid();
    }

    private Uni<Void> failWorkflow(WorkflowRun run, ExecutionError error) {
        run.setState(WorkflowRunState.FAILED);
        run.setErrorMessage(error.getMessage());
        run.setCompletedAt(Instant.now());

        return runRepository.save(run)
                .onItem().invoke(savedRun -> {
                    activeRuns.remove(run.getId());
                    scheduler.cancelAllForRun(run.getId());

                    eventPublisher.publish(new WorkflowRunFailed(run.getId(), error));

                    if (error.getCompensationHint() != null) {
                        initiateCompensation(savedRun);
                    }
                })
                .replaceWithVoid();
    }

    private void initiateCompensation(WorkflowRun run) {
        run.setState(WorkflowRunState.COMPENSATING);

        runRepository.save(run)
                .subscribe().with(
                        savedRun -> executeCompensation(savedRun),
                        failure -> LOG.errorf("Failed to initiate compensation: %s", failure));
    }

    private void validateStateTransition(
            WorkflowRunState from,
            WorkflowRunState to) {

        // State transition validation logic
        if (from == to)
            return;

        if (isTerminalState(from)) {
            throw new IllegalStateException(
                    String.format("Cannot transition from terminal state %s to %s", from, to));
        }

        // Define valid transitions
        boolean valid = switch (from) {
            case CREATED -> to == WorkflowRunState.RUNNING ||
                    to == WorkflowRunState.CANCELLED;
            case RUNNING -> to == WorkflowRunState.WAITING ||
                    to == WorkflowRunState.RETRYING ||
                    to == WorkflowRunState.COMPLETED ||
                    to == WorkflowRunState.FAILED ||
                    to == WorkflowRunState.CANCELLED;
            case WAITING -> to == WorkflowRunState.RUNNING ||
                    to == WorkflowRunState.CANCELLED;
            case RETRYING -> to == WorkflowRunState.RUNNING ||
                    to == WorkflowRunState.FAILED;
            case COMPENSATING -> to == WorkflowRunState.FAILED ||
                    to == WorkflowRunState.CANCELLED;
            default -> false;
        };

        if (!valid) {
            throw new IllegalStateException(
                    String.format("Invalid state transition from %s to %s", from, to));
        }
    }

    private boolean isTerminalState(WorkflowRunState state) {
        return switch (state) {
            case COMPLETED, FAILED, CANCELLED -> true;
            default -> false;
        };
    }

    // ================ INNER CLASSES ================

    private static class ActiveRunState {
        private WorkflowRun run;
        private final Map<String, Integer> retryCounts = new HashMap<>();
        private final Map<String, Instant> lastFailureTimes = new HashMap<>();
        private WaitInfo waitInfo;

    }
}
