package tech.kayys.wayang.workflow.kernel;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.workflow.kernel.*;
import tech.kayys.wayang.workflow.saga.service.WorkflowSagaCoordinator;
import tech.kayys.wayang.workflow.scheduler.service.WorkflowScheduler;
import tech.kayys.wayang.workflow.api.model.RunStatus;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.workflow.api.dto.*;
import tech.kayys.wayang.sdk.util.WorkflowValidator.ValidationResult;
import java.time.Duration;
import java.util.*;

/**
 * Default implementation of WorkflowRunManager using the abstract base
 */
@ApplicationScoped
public class DefaultWorkflowRunManager extends AbstractWorkflowRunManager {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultWorkflowRunManager.class);

    @Inject
    protected WorkflowSagaCoordinator sagaCoordinator;

    @Inject
    protected WorkflowScheduler workflowScheduler;

    @Inject
    protected RetryPolicyManager retryPolicyManager;

    @Override
    public Uni<WorkflowRunId> createRun(
            WorkflowDescriptor workflow,
            Map<String, Object> inputs,
            RunTrigger trigger) {

        // Enhanced creation with workflow validation
        return validateWorkflowDefinition(workflow)
                .flatMap(validation -> {
                    if (!validation.isValid()) {
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("Invalid workflow: " + validation.getMessage()));
                    }

                    // Apply input transformation if needed
                    Map<String, Object> processedInputs = processInputs(inputs, workflow);

                    // Delegate to parent implementation
                    return super.createRun(workflow, processedInputs, trigger)
                            .onItem().invoke(runId -> {
                                // Schedule any time-based triggers
                                scheduleWorkflowTriggers(runId, workflow);
                            });
                });
    }

    @Override
    public Uni<Void> handleNodeResult(
            WorkflowRunId runId,
            NodeExecutionResult result) {

        return super.handleNodeResult(runId, result)
                .onItem().transformToUni(v -> {
                    // Check if retry is needed
                    if (result.getStatus() == NodeExecutionStatus.FAILED) {
                        return handleNodeFailure(runId, result);
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    @Override
    public Uni<Void> cancelRun(
            WorkflowRunId runId,
            CancelReason reason) {

        // Enhanced cancellation with saga compensation
        return super.cancelRun(runId, reason)
                .onItem().transformToUni(v -> {
                    // Execute saga compensation for distributed transactions
                    return sagaCoordinator.compensateRun(runId, reason)
                            .onFailure()
                            .invoke(th -> LOG.error("Saga compensation failed for run {}", runId.value(), th))
                            .replaceWithVoid();
                });
    }

    @Override
    protected WorkflowDecision makeWorkflowDecision(WorkflowRunState runState,
            NodeExecutionResult result) {
        // Enhanced decision making with workflow definition awareness
        WorkflowDescriptor workflow = getWorkflowDefinition(runState.getWorkflowId());

        if (workflow == null) {
            LOG.error("Workflow definition not found for {}", runState.getWorkflowId());
            return WorkflowDecision.fail("Workflow definition not found");
        }

        // Check if this node failure should fail the entire workflow
        if (result.getStatus() == NodeExecutionStatus.FAILED) {
            if (isCriticalNode(workflow, result.getNodeId())) {
                return WorkflowDecision.fail("Critical node failed: " + result.getErrorMessage());
            }
            // Non-critical failures might allow workflow to continue
            return WorkflowDecision.continueWorkflow();
        }

        // Check if workflow is complete
        if (isWorkflowComplete(runState, workflow)) {
            Map<String, Object> outputs = collectWorkflowOutputs(runState, workflow);
            return WorkflowDecision.complete(outputs);
        }

        // Check if should wait for external signal
        if (requiresExternalSignal(workflow, result.getNodeId())) {
            return WorkflowDecision.suspend();
        }

        return WorkflowDecision.continueWorkflow();
    }

    @Override
    protected Uni<CompensationResult> executeCompensation(WorkflowRunState runState) {
        // Enhanced compensation with multiple strategies
        return Uni.createFrom().deferred(() -> {
            CompensationStrategy strategy = selectCompensationStrategy(runState);

            return strategy.execute(runState)
                    .onFailure()
                    .recoverWithItem(th -> new CompensationResult(false, "Compensation failed: " + th.getMessage()));
        });
    }

    // Additional helper methods
    private Uni<ValidationResult> validateWorkflowDefinition(WorkflowDescriptor workflow) {
        return Uni.createFrom().deferred(() -> {
            List<String> errors = new ArrayList<>();

            if (workflow == null) {
                errors.add("Workflow cannot be null");
                return Uni.createFrom().item(ValidationResult.failure(String.join("; ", errors)));
            }

            if (workflow.getId() == null || workflow.getId().isEmpty()) {
                errors.add("Workflow ID cannot be empty");
            }

            if (workflow.getNodes() == null || workflow.getNodes().isEmpty()) {
                errors.add("Workflow must contain at least one node");
            }

            // Validate node dependencies form a DAG
            if (!isValidDAG(workflow.getNodes())) {
                errors.add("Workflow nodes must form a Directed Acyclic Graph");
            }

            if (errors.isEmpty()) {
                return Uni.createFrom().item(ValidationResult.success());
            } else {
                return Uni.createFrom().item(ValidationResult.failure(String.join("; ", errors)));
            }
        });
    }

    private Map<String, Object> processInputs(Map<String, Object> inputs,
            WorkflowDescriptor workflow) {
        Map<String, Object> processed = new HashMap<>(inputs != null ? inputs : Map.of());

        // Apply input transformations defined in workflow
        if (workflow.getInputTransformations() != null) {
            workflow.getInputTransformations().forEach((key, transformation) -> {
                if (processed.containsKey(key)) {
                    Object transformed = applyTransformation(processed.get(key), transformation);
                    processed.put(key, transformed);
                }
            });
        }

        // Set default values for missing required inputs
        if (workflow.getRequiredInputs() != null) {
            workflow.getRequiredInputs().forEach((key, defaultValue) -> {
                if (!processed.containsKey(key)) {
                    processed.put(key, defaultValue);
                }
            });
        }

        return processed;
    }

    private void scheduleWorkflowTriggers(WorkflowRunId runId, WorkflowDescriptor workflow) {
        if (workflow.getTimeBasedTriggers() != null) {
            workflow.getTimeBasedTriggers().forEach(trigger -> {
                workflowScheduler.schedule(runId, trigger)
                        .subscribe().with(
                                success -> LOG.debug("Scheduled trigger for run {}", runId.value()),
                                failure -> LOG.error("Failed to schedule trigger for run {}", runId.value(), failure));
            });
        }
    }

    private Uni<Void> handleNodeFailure(WorkflowRunId runId, NodeExecutionResult result) {
        return retryPolicyManager.shouldRetry(runId, result.getNodeId(), result.getAttempt())
                .flatMap(shouldRetry -> {
                    if (shouldRetry) {
                        Duration delay = retryPolicyManager.getRetryDelay(
                                runId, result.getNodeId(), result.getAttempt());

                        LOG.info("Scheduling retry for node {} in run {} after {} ms",
                                result.getNodeId(), runId.value(), delay.toMillis());

                        return workflowScheduler.scheduleRetry(runId, result.getNodeId(), delay)
                                .replaceWithVoid();
                    } else {
                        LOG.warn("Max retries exceeded for node {} in run {}",
                                result.getNodeId(), runId.value());
                        return Uni.createFrom().voidItem();
                    }
                });
    }

    private boolean isValidDAG(List<NodeDescriptor> nodes) {
        // Implementation for DAG validation
        return true; // Simplified
    }

    private Object applyTransformation(Object value, Transformation transformation) {
        // Implementation for input transformation
        return value; // Simplified
    }

    private WorkflowDescriptor getWorkflowDefinition(String workflowId) {
        // Implementation to retrieve workflow definition
        return null; // Simplified
    }

    private boolean isCriticalNode(WorkflowDescriptor workflow, String nodeId) {
        // Implementation to check if node is critical
        return true; // Simplified
    }

    private boolean isWorkflowComplete(WorkflowRunState runState, WorkflowDescriptor workflow) {
        // Implementation to check workflow completion
        return false; // Simplified
    }

    private Map<String, Object> collectWorkflowOutputs(WorkflowRunState runState,
            WorkflowDescriptor workflow) {
        // Implementation to collect final outputs
        return new HashMap<>(); // Simplified
    }

    private boolean requiresExternalSignal(WorkflowDescriptor workflow, String nodeId) {
        // Implementation to check if node requires external signal
        return false; // Simplified
    }

    private CompensationStrategy selectCompensationStrategy(WorkflowRunState runState) {
        // Implementation to select appropriate compensation strategy
        return new DefaultCompensationStrategy(); // Simplified
    }

    private interface CompensationStrategy {
        Uni<CompensationResult> execute(WorkflowRunState runState);
    }

    private class DefaultCompensationStrategy implements CompensationStrategy {
        @Override
        public Uni<CompensationResult> execute(WorkflowRunState runState) {
            return Uni.createFrom().item(new CompensationResult(true, "Default compensation executed"));
        }
    }

    @Override
    public Uni<tech.kayys.wayang.workflow.domain.WorkflowRun> createRun(
            tech.kayys.wayang.workflow.api.dto.CreateRunRequest request, String tenantId) {
        throw new UnsupportedOperationException("Unimplemented method 'createRun'");
    }

    @Override
    public Uni<tech.kayys.wayang.workflow.domain.WorkflowRun> getRun(String runId) {
        throw new UnsupportedOperationException("Unimplemented method 'getRun'");
    }

    @Override
    public Uni<tech.kayys.wayang.workflow.domain.WorkflowRun> startRun(String runId, String tenantId) {
        throw new UnsupportedOperationException("Unimplemented method 'startRun'");
    }

    @Override
    public Uni<tech.kayys.wayang.workflow.domain.WorkflowRun> suspendRun(String runId, String tenantId, String reason,
            String humanTaskId) {
        throw new UnsupportedOperationException("Unimplemented method 'suspendRun'");
    }

    @Override
    public Uni<tech.kayys.wayang.workflow.domain.WorkflowRun> resumeRun(String runId, String tenantId,
            String humanTaskId,
            Map<String, Object> resumeData) {
        throw new UnsupportedOperationException("Unimplemented method 'resumeRun'");
    }

    @Override
    public Uni<Void> cancelRun(String runId, String tenantId, String reason) {
        throw new UnsupportedOperationException("Unimplemented method 'cancelRun'");
    }

    @Override
    public Uni<tech.kayys.wayang.workflow.domain.WorkflowRun> completeRun(String runId, String tenantId,
            Map<String, Object> outputs) {
        throw new UnsupportedOperationException("Unimplemented method 'completeRun'");
    }

    @Override
    public Uni<tech.kayys.wayang.workflow.domain.WorkflowRun> failRun(String runId, String tenantId,
            tech.kayys.wayang.workflow.api.dto.ErrorResponse error) {
        throw new UnsupportedOperationException("Unimplemented method 'failRun'");
    }

    @Override
    public Uni<java.util.List<tech.kayys.wayang.workflow.api.dto.RunResponse>> queryRuns(String tenantId,
            String workflowId,
            tech.kayys.wayang.workflow.api.model.RunStatus status, int page, int size) {
        throw new UnsupportedOperationException("Unimplemented method 'queryRuns'");
    }

    @Override
    public Uni<Long> getActiveRunsCount(String tenantId) {
        throw new UnsupportedOperationException("Unimplemented method 'getActiveRunsCount'");
    }
}
