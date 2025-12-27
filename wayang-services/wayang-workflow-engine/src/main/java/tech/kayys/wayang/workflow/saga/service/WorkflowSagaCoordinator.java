package tech.kayys.wayang.workflow.saga.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.sdk.dto.NodeExecutionState;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.workflow.saga.model.CompensationAction;
import tech.kayys.wayang.workflow.saga.model.CompensationActionDefinition;
import tech.kayys.wayang.workflow.saga.model.SagaDefinition;
import tech.kayys.wayang.workflow.saga.model.SagaExecution;
import tech.kayys.wayang.workflow.saga.model.SagaStatus;
import tech.kayys.wayang.workflow.saga.repository.SagaExecutionRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Saga Coordinator for Workflow Compensation
 *
 * Implements Saga pattern for distributed transactions:
 * - Forward recovery: retry failed operations
 * - Backward recovery: compensate completed operations
 * - Pivot point detection: point of no return
 *
 * Example Saga:
 * T1: Reserve Inventory -> C1: Release Inventory
 * T2: Charge Payment -> C2: Refund Payment
 * T3: Send Confirmation -> C3: Send Cancellation
 *
 * If T2 fails: Execute C1
 * If cancelled after T2: Execute C2, C1
 */
@ApplicationScoped
public class WorkflowSagaCoordinator {

    private static final Logger log = LoggerFactory.getLogger(WorkflowSagaCoordinator.class);

    @Inject
    SagaExecutionRepository sagaRepository;

    @Inject
    SagaDefinitionRegistry sagaRegistry;

    @Inject
    CompensationExecutor compensationExecutor;

    /**
     * Handle node failure with saga compensation
     */
    public Uni<Void> handleNodeFailure(WorkflowRun run, NodeExecutionState failedNode) {
        log.info("Handling node failure in run {}, node: {}",
                run.getRunId(), failedNode.nodeId());

        return executeCompensation(run);
    }

    /**
     * Compensate cancelled workflow run
     */
    public Uni<Void> compensateCancelledRun(WorkflowRun run) {
        log.info("Compensating cancelled run: {}", run.getRunId());

        return executeCompensation(run);
    }

    /**
     * Execute compensation strategy
     */
    private Uni<Void> executeCompensation(WorkflowRun run) {
        log.debug("Initiating compensation for run: {}", run.getRunId());

        return sagaRegistry.getSagaDefinition(run.getWorkflowId(), run.getTenantId())
                .onItem().ifNotNull().transformToUni(sagaDef -> {
                    log.info("Found saga definition: {} for workflow: {}", sagaDef.getId(), run.getWorkflowId());

                    // Create Saga Execution record
                    SagaExecution execution = new SagaExecution(
                            UUID.randomUUID().toString(),
                            run.getRunId(),
                            sagaDef.getId(),
                            sagaDef.getCompensationStrategy(),
                            SagaStatus.STARTED,
                            java.time.Instant.now());

                    return sagaRepository.save(execution)
                            .chain(savedExec -> performBackwardRecovery(run, sagaDef)
                                    .onItem().transformToUni(v -> {
                                        savedExec.complete();
                                        return sagaRepository.update(savedExec).replaceWithVoid();
                                    })
                                    .onFailure().recoverWithUni(error -> {
                                        log.error("Compensation failed for run: {}", run.getRunId(), error);
                                        savedExec.fail(error.getMessage());
                                        return sagaRepository.update(savedExec).replaceWithVoid();
                                    }));
                })
                .onItem().ifNull().continueWith(() -> {
                    log.debug("No saga definition found for workflow: {}. Skipping compensation.", run.getWorkflowId());
                    return null;
                })
                .replaceWithVoid();
    }

    /**
     * Perform backward recovery by compensating executed nodes in reverse order
     * (LIFO)
     */
    private Uni<Void> performBackwardRecovery(WorkflowRun run, SagaDefinition sagaDef) {
        if (run.getExecutionState() == null || run.getExecutionState().getExecutedNodes().isEmpty()) {
            log.info("No nodes executed in run {}. Nothing to compensate.", run.getRunId());
            return Uni.createFrom().voidItem();
        }

        List<String> executedNodes = new ArrayList<>(run.getExecutionState().getExecutedNodes());
        Collections.reverse(executedNodes);

        log.info("Starting backward recovery for run {}. Compensation path: {}", run.getRunId(), executedNodes);

        Uni<Void> result = Uni.createFrom().voidItem();

        for (String nodeId : executedNodes) {
            CompensationActionDefinition actionDef = sagaDef.getCompensations().get(nodeId);
            if (actionDef != null) {
                log.info("Executing compensation for node: {}", nodeId);
                CompensationAction action = new CompensationAction(
                        actionDef.getActionType(),
                        actionDef.getParameters());

                result = result.chain(v -> compensationExecutor.execute(run, nodeId, action));
            } else {
                log.debug("No compensation defined for node: {}. Skipping.", nodeId);
            }

            // If we reached the pivot node, we might want to stop or change strategy
            // For now, we continue as defined by the strategy (BACKWARD usually means
            // full)
            if (nodeId.equals(sagaDef.getPivotNode())) {
                log.info("Reached pivot node: {}. Continuing compensation according to strategy.", nodeId);
            }
        }

        return result;
    }
}
