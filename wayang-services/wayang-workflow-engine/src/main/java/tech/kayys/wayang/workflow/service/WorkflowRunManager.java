package tech.kayys.wayang.workflow.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.cache.CacheResult;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import tech.kayys.wayang.schema.execution.ErrorPayload;
import tech.kayys.wayang.sdk.dto.NodeExecutionState;
import tech.kayys.wayang.workflow.api.dto.CreateRunRequest;
import tech.kayys.wayang.workflow.api.model.RunStatus;
import tech.kayys.wayang.workflow.api.model.WorkflowEventType;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.workflow.model.ClusterEvent;
import tech.kayys.wayang.workflow.model.DistributedLock;
import tech.kayys.wayang.workflow.api.model.WorkflowEvent;
import tech.kayys.wayang.workflow.model.WorkflowRunQuery;
import tech.kayys.wayang.workflow.model.WorkflowSnapshot;
import tech.kayys.wayang.workflow.repository.WorkflowRunRepository;
import tech.kayys.wayang.workflow.exception.RunNotFoundException;

/**
 * WorkflowRunManager
 */
@ApplicationScoped
public class WorkflowRunManager {

        private static final Logger log = LoggerFactory.getLogger(WorkflowRunManager.class);
        private static final String EVENT_BUS_ADDRESS = "workflow.runs";
        private static final Duration STALE_RUN_THRESHOLD = Duration.ofHours(24);

        @Inject
        WorkflowRunRepository runRepository;

        @Inject
        WorkflowEventStore eventStore;

        @Inject
        WorkflowSnapshotStore snapshotStore;

        @Inject
        WorkflowSagaCoordinator sagaCoordinator;

        @Inject
        DistributedLockManager lockManager;

        @Inject
        EventBus eventBus;

        @Inject
        ProvenanceService provenanceService;

        @Inject
        CacheManager cacheManager;

        private final Map<String, WorkflowRun> activeRunsCache = new ConcurrentHashMap<>();

        // ========================================================================
        // COMMAND SIDE: State Mutations
        // ========================================================================

        @Transactional
        public Uni<WorkflowRun> createRun(CreateRunRequest request) {
                String tenantId = "default-tenant";
                log.info("Creating workflow run for workflow: {}, tenant: {}", request.getWorkflowId(), tenantId);

                // Validate input
                if (request.getWorkflowId() == null || request.getWorkflowId().trim().isEmpty()) {
                        return Uni.createFrom()
                                        .failure(new IllegalArgumentException("Workflow ID cannot be null or empty"));
                }

                WorkflowRun run = WorkflowRun.create(
                                request.getWorkflowId(),
                                request.getWorkflowVersion(),
                                tenantId,
                                request.getInputs(),
                                "manual",
                                "manual");

                WorkflowEvent createdEvent = WorkflowEvent.created(run, request.getInputs());

                return eventStore.append(run.getRunId(), createdEvent)
                                .onItem().transformToUni(eventId -> runRepository.save(run))
                                .onFailure().invoke(failure -> {
                                        log.error("Failed to create workflow run for workflow: {}",
                                                        request.getWorkflowId(), failure);
                                })
                                .onItem().invoke(savedRun -> {
                                        activeRunsCache.put(savedRun.getRunId(), savedRun);
                                        publishClusterEvent(new ClusterEvent.RunCreated(savedRun.getRunId(),
                                                        savedRun.getWorkflowId()));
                                        logProvenance(savedRun.getRunId(), "RUN_CREATED",
                                                        Map.of("workflowId", request.getWorkflowId(), "tenantId",
                                                                        tenantId));
                                });
        }

        @Transactional
        public Uni<WorkflowRun> startRun(String runId, String tenantId) {
                if (runId == null || runId.trim().isEmpty()) {
                        return Uni.createFrom().failure(new IllegalArgumentException("Run ID cannot be null or empty"));
                }
                if (tenantId == null || tenantId.trim().isEmpty()) {
                        return Uni.createFrom()
                                        .failure(new IllegalArgumentException("Tenant ID cannot be null or empty"));
                }

                return runRepository.findById(runId)
                                .onItem().ifNull().failWith(() -> new RunNotFoundException(runId))
                                .onItem().invoke(run -> {
                                        if (!run.getTenantId().equals(tenantId)) {
                                                throw new IllegalStateException("Tenant mismatch for run: " + runId);
                                        }
                                })
                                .chain(run -> updateRunStatus(runId, tenantId, RunStatus.RUNNING))
                                .onFailure()
                                .invoke(failure -> log.error("Failed to start workflow run: {}", runId, failure))
                                .chain(v -> getRun(runId));
        }

        @Transactional
        public Uni<WorkflowRun> suspendRun(String runId, String tenantId, String reason, String humanTaskId) {
                if (runId == null || runId.trim().isEmpty()) {
                        return Uni.createFrom().failure(new IllegalArgumentException("Run ID cannot be null or empty"));
                }
                if (tenantId == null || tenantId.trim().isEmpty()) {
                        return Uni.createFrom()
                                        .failure(new IllegalArgumentException("Tenant ID cannot be null or empty"));
                }

                return runRepository.findById(runId)
                                .onItem().ifNull().failWith(() -> new RunNotFoundException(runId))
                                .onItem().invoke(run -> {
                                        if (!run.getTenantId().equals(tenantId)) {
                                                throw new IllegalStateException("Tenant mismatch for run: " + runId);
                                        }
                                })
                                .chain(run -> updateRunStatus(runId, tenantId, RunStatus.SUSPENDED))
                                .onFailure()
                                .invoke(failure -> log.error("Failed to suspend workflow run: {}", runId, failure))
                                .chain(v -> getRun(runId));
        }

        @Transactional
        public Uni<Void> updateRunStatus(String runId, String tenantId, RunStatus newStatus) {
                if (runId == null || runId.trim().isEmpty()) {
                        return Uni.createFrom().failure(new IllegalArgumentException("Run ID cannot be null or empty"));
                }
                if (tenantId == null || tenantId.trim().isEmpty()) {
                        return Uni.createFrom()
                                        .failure(new IllegalArgumentException("Tenant ID cannot be null or empty"));
                }

                return acquireLock(runId, "status-update")
                                .onItem().transformToUni(lock -> runRepository.findById(runId)
                                                .onItem().ifNull().failWith(() -> new RunNotFoundException(runId))
                                                .onItem().invoke(run -> {
                                                        if (!run.getTenantId().equals(tenantId)) {
                                                                throw new IllegalStateException(
                                                                                "Tenant mismatch for run: " + runId);
                                                        }
                                                })
                                                .onItem().transformToUni(run -> {
                                                        RunStatus oldStatus = run.getStatus();
                                                        validateStateTransition(oldStatus, newStatus);

                                                        WorkflowEvent statusEvent = WorkflowEvent.statusChanged(runId,
                                                                        oldStatus, newStatus);

                                                        return eventStore.append(runId, statusEvent)
                                                                        .onItem().transformToUni(eventId -> {
                                                                                run.setStatus(newStatus);
                                                                                return runRepository
                                                                                                .updateWithOptimisticLock(
                                                                                                                run);
                                                                        })
                                                                        .onFailure().invoke(failure -> {
                                                                                log.error("Failed to update run status for run: {}, from: {} to: {}",
                                                                                                runId, oldStatus,
                                                                                                newStatus, failure);
                                                                        })
                                                                        .onItem().invoke(v -> {
                                                                                cacheManager.invalidate("workflow-run",
                                                                                                runId);
                                                                                activeRunsCache.remove(runId);
                                                                                publishClusterEvent(
                                                                                                new ClusterEvent.StatusChanged(
                                                                                                                runId,
                                                                                                                newStatus));
                                                                                logProvenance(runId, "STATUS_CHANGED",
                                                                                                Map.of("oldStatus",
                                                                                                                oldStatus,
                                                                                                                "newStatus",
                                                                                                                newStatus));
                                                                        })
                                                                        .replaceWithVoid();
                                                })
                                                .eventually(() -> releaseLock(lock)));
        }

        @Transactional
        public Uni<Void> recordNodeState(String runId, String tenantId, NodeExecutionState nodeState) {
                if (runId == null || runId.trim().isEmpty()) {
                        return Uni.createFrom().failure(new IllegalArgumentException("Run ID cannot be null or empty"));
                }
                if (tenantId == null || tenantId.trim().isEmpty()) {
                        return Uni.createFrom()
                                        .failure(new IllegalArgumentException("Tenant ID cannot be null or empty"));
                }
                if (nodeState == null) {
                        return Uni.createFrom().failure(new IllegalArgumentException("Node state cannot be null"));
                }

                return acquireLock(runId, "node-update")
                                .onItem().transformToUni(lock -> runRepository.findById(runId)
                                                .onItem().ifNull().failWith(() -> new RunNotFoundException(runId))
                                                .onItem().invoke(run -> {
                                                        if (!run.getTenantId().equals(tenantId)) {
                                                                throw new IllegalStateException(
                                                                                "Tenant mismatch for run: " + runId);
                                                        }
                                                })
                                                .onItem().transformToUni(run -> {
                                                        WorkflowEvent nodeEvent = WorkflowEvent.nodeExecuted(runId,
                                                                        nodeState);

                                                        final Uni<Void> sagaOperation = nodeState
                                                                        .status() == NodeExecutionState.NodeStatus.FAILED
                                                                                        ? sagaCoordinator
                                                                                                        .handleNodeFailure(
                                                                                                                        run,
                                                                                                                        nodeState)
                                                                                        : Uni.createFrom().voidItem();

                                                        return eventStore.append(runId, nodeEvent)
                                                                        .onItem().transformToUni(eventId -> {
                                                                                run.recordNodeState(nodeState.nodeId(),
                                                                                                nodeState);
                                                                                return runRepository.update(run);
                                                                        })
                                                                        .onFailure().invoke(failure -> {
                                                                                log.error("Failed to record node state for run: {}, node: {}",
                                                                                                runId,
                                                                                                nodeState.nodeId(),
                                                                                                failure);
                                                                        })
                                                                        .onItem().transformToUni(v -> sagaOperation)
                                                                        .onFailure()
                                                                        .invoke(failure -> log.error(
                                                                                        "Failed to handle node failure for run: {}, node: {}",
                                                                                        runId, nodeState.nodeId(),
                                                                                        failure))
                                                                        .onItem().invoke(v -> {
                                                                                publishClusterEvent(
                                                                                                new ClusterEvent.NodeExecuted(
                                                                                                                runId,
                                                                                                                nodeState.nodeId()));
                                                                                logProvenance(runId, "NODE_EXECUTED",
                                                                                                Map.of("nodeId", nodeState
                                                                                                                .nodeId(),
                                                                                                                "status",
                                                                                                                nodeState.status()));
                                                                        });
                                                })
                                                .eventually(() -> releaseLock(lock)));
        }

        @Transactional
        public Uni<WorkflowRun> completeRun(String runId, String tenantId, Map<String, Object> outputs) {
                log.info("Completing workflow run: {}", runId);

                if (runId == null || runId.trim().isEmpty()) {
                        return Uni.createFrom().failure(new IllegalArgumentException("Run ID cannot be null or empty"));
                }
                if (tenantId == null || tenantId.trim().isEmpty()) {
                        return Uni.createFrom()
                                        .failure(new IllegalArgumentException("Tenant ID cannot be null or empty"));
                }

                return acquireLock(runId, "completion")
                                .onItem().transformToUni(lock -> runRepository.findById(runId)
                                                .onItem().ifNull().failWith(() -> new RunNotFoundException(runId))
                                                .onItem().invoke(run -> {
                                                        if (!run.getTenantId().equals(tenantId)) {
                                                                throw new IllegalStateException(
                                                                                "Tenant mismatch for run: " + runId);
                                                        }
                                                        // Validate state transition
                                                        validateStateTransition(run.getStatus(), RunStatus.SUCCEEDED);
                                                })
                                                .onItem().transformToUni(run -> {
                                                        WorkflowEvent completedEvent = WorkflowEvent.completed(runId,
                                                                        outputs);

                                                        return eventStore.append(runId, completedEvent)
                                                                        .onItem().transformToUni(eventId -> {
                                                                                run.setStatus(RunStatus.SUCCEEDED);
                                                                                run.setOutputs(outputs);
                                                                                run.complete(outputs);
                                                                                return runRepository.update(run);
                                                                        })
                                                                        .onFailure().invoke(failure -> {
                                                                                log.error("Failed to complete run: {}",
                                                                                                runId, failure);
                                                                        })
                                                                        .onItem().invoke(v -> {
                                                                                cacheManager.invalidate("workflow-run",
                                                                                                runId);
                                                                                activeRunsCache.remove(runId);
                                                                                publishClusterEvent(
                                                                                                new ClusterEvent.RunCompleted(
                                                                                                                runId));
                                                                                logProvenance(runId, "RUN_COMPLETED",
                                                                                                Map.of("outputs",
                                                                                                                outputs != null ? outputs
                                                                                                                                : Map.of()));
                                                                        });
                                                })
                                                .eventually(() -> releaseLock(lock)));
        }

        @Transactional
        public Uni<WorkflowRun> failRun(String runId, String tenantId, ErrorPayload error) {
                log.info("Failing workflow run: {}", runId);

                if (runId == null || runId.trim().isEmpty()) {
                        return Uni.createFrom().failure(new IllegalArgumentException("Run ID cannot be null or empty"));
                }
                if (tenantId == null || tenantId.trim().isEmpty()) {
                        return Uni.createFrom()
                                        .failure(new IllegalArgumentException("Tenant ID cannot be null or empty"));
                }

                return acquireLock(runId, "failure")
                                .onItem().transformToUni(lock -> runRepository.findById(runId)
                                                .onItem().ifNull().failWith(() -> new RunNotFoundException(runId))
                                                .onItem().invoke(run -> {
                                                        if (!run.getTenantId().equals(tenantId)) {
                                                                throw new IllegalStateException(
                                                                                "Tenant mismatch for run: " + runId);
                                                        }
                                                        // Validate state transition
                                                        validateStateTransition(run.getStatus(), RunStatus.FAILED);
                                                })
                                                .onItem().transformToUni(run -> {
                                                        WorkflowEvent statusEvent = WorkflowEvent.statusChanged(runId,
                                                                        run.getStatus(), RunStatus.FAILED);

                                                        return eventStore.append(runId, statusEvent)
                                                                        .onItem().transformToUni(eventId -> {
                                                                                run.setStatus(RunStatus.FAILED);
                                                                                run.fail(error);
                                                                                return runRepository.update(run);
                                                                        })
                                                                        .onFailure().invoke(failure -> {
                                                                                log.error("Failed to fail run: {}",
                                                                                                runId, failure);
                                                                        })
                                                                        .onItem().invoke(v -> {
                                                                                cacheManager.invalidate("workflow-run",
                                                                                                runId);
                                                                                activeRunsCache.remove(runId);
                                                                                publishClusterEvent(
                                                                                                new ClusterEvent.StatusChanged(
                                                                                                                runId,
                                                                                                                RunStatus.FAILED));
                                                                                logProvenance(runId, "RUN_FAILED",
                                                                                                Map.of("error", error != null
                                                                                                                ? error.getMessage()
                                                                                                                : "Unknown error"));
                                                                        });
                                                })
                                                .eventually(() -> releaseLock(lock)));
        }

        @Transactional
        public Uni<Void> updateWorkflowState(String runId, String tenantId, Map<String, Object> stateUpdates) {
                if (runId == null || runId.trim().isEmpty()) {
                        return Uni.createFrom().failure(new IllegalArgumentException("Run ID cannot be null or empty"));
                }
                if (tenantId == null || tenantId.trim().isEmpty()) {
                        return Uni.createFrom()
                                        .failure(new IllegalArgumentException("Tenant ID cannot be null or empty"));
                }

                return acquireLock(runId, "state-update")
                                .onItem().transformToUni(lock -> runRepository.findById(runId)
                                                .onItem().ifNull().failWith(() -> new RunNotFoundException(runId))
                                                .onItem().invoke(run -> {
                                                        if (!run.getTenantId().equals(tenantId)) {
                                                                throw new IllegalStateException(
                                                                                "Tenant mismatch for run: " + runId);
                                                        }
                                                })
                                                .onItem().transformToUni(run -> {
                                                        WorkflowEvent stateEvent = WorkflowEvent.stateUpdated(runId,
                                                                        stateUpdates);

                                                        return eventStore.append(runId, stateEvent)
                                                                        .onItem().transformToUni(eventId -> {
                                                                                run.updateWorkflowState(stateUpdates);
                                                                                return runRepository.update(run);
                                                                        })
                                                                        .onFailure().invoke(failure -> {
                                                                                log.error("Failed to update workflow state for run: {}",
                                                                                                runId, failure);
                                                                        })
                                                                        .onItem().invoke(v -> {
                                                                                cacheManager.invalidate("workflow-run",
                                                                                                runId);
                                                                                publishClusterEvent(
                                                                                                new ClusterEvent.StateUpdated(
                                                                                                                runId));
                                                                                logProvenance(runId,
                                                                                                "WORKFLOW_STATE_UPDATED",
                                                                                                Map.of("stateUpdates",
                                                                                                                stateUpdates != null
                                                                                                                                ? stateUpdates
                                                                                                                                : Map.of()));
                                                                        })
                                                                        .replaceWithVoid();
                                                })
                                                .eventually(() -> releaseLock(lock)));
        }

        @Transactional
        public Uni<WorkflowRun> resumeRun(String runId, String tenantId, String humanTaskId,
                        Map<String, Object> resumeData) {
                log.info("Resuming workflow run: {}, task: {}", runId, humanTaskId);

                if (runId == null || runId.trim().isEmpty()) {
                        return Uni.createFrom().failure(new IllegalArgumentException("Run ID cannot be null or empty"));
                }
                if (tenantId == null || tenantId.trim().isEmpty()) {
                        return Uni.createFrom()
                                        .failure(new IllegalArgumentException("Tenant ID cannot be null or empty"));
                }

                return acquireLock(runId, "resume")
                                .onItem().transformToUni(lock -> runRepository.findById(runId)
                                                .onItem().ifNull().failWith(() -> new RunNotFoundException(runId))
                                                .onItem().invoke(run -> {
                                                        if (!run.getTenantId().equals(tenantId)) {
                                                                throw new IllegalStateException(
                                                                                "Tenant mismatch for run: " + runId);
                                                        }
                                                })
                                                .onItem().transformToUni(run -> {
                                                        WorkflowEvent resumeEvent = WorkflowEvent.resumed(runId,
                                                                        humanTaskId, resumeData, "user");

                                                        return eventStore.append(runId, resumeEvent)
                                                                        .onItem().transformToUni(eventId -> {
                                                                                run.setStatus(RunStatus.RUNNING);
                                                                                if (resumeData != null) {
                                                                                        run.updateWorkflowState(
                                                                                                        resumeData);
                                                                                }
                                                                                return runRepository.update(run);
                                                                        })
                                                                        .onFailure().invoke(failure -> {
                                                                                log.error("Failed to resume workflow run: {}",
                                                                                                runId, failure);
                                                                        })
                                                                        .onItem().invoke(updatedRun -> {
                                                                                cacheManager.invalidate("workflow-run",
                                                                                                runId);
                                                                                publishClusterEvent(
                                                                                                new ClusterEvent.RunResumed(
                                                                                                                runId,
                                                                                                                humanTaskId));
                                                                                logProvenance(runId, "RUN_RESUMED",
                                                                                                Map.of("humanTaskId",
                                                                                                                humanTaskId));
                                                                        });
                                                })
                                                .eventually(() -> releaseLock(lock)));
        }

        @Transactional
        public Uni<Void> cancelRun(String runId, String tenantId, String reason) {
                log.info("Cancelling workflow run: {}, reason: {}", runId, reason);

                if (runId == null || runId.trim().isEmpty()) {
                        return Uni.createFrom().failure(new IllegalArgumentException("Run ID cannot be null or empty"));
                }
                if (tenantId == null || tenantId.trim().isEmpty()) {
                        return Uni.createFrom()
                                        .failure(new IllegalArgumentException("Tenant ID cannot be null or empty"));
                }

                return acquireLock(runId, "cancel")
                                .onItem().transformToUni(lock -> runRepository.findById(runId)
                                                .onItem().ifNull().failWith(() -> new RunNotFoundException(runId))
                                                .onItem().invoke(run -> {
                                                        if (!run.getTenantId().equals(tenantId)) {
                                                                throw new IllegalStateException(
                                                                                "Tenant mismatch for run: " + runId);
                                                        }
                                                })
                                                .onItem().transformToUni(run -> {
                                                        WorkflowEvent cancelEvent = WorkflowEvent.cancelled(runId,
                                                                        reason != null ? reason : "Cancelled by user",
                                                                        "user");

                                                        return eventStore.append(runId, cancelEvent)
                                                                        .onItem().transformToUni(eventId -> {
                                                                                run.setStatus(RunStatus.CANCELLED);
                                                                                return runRepository.update(run);
                                                                        })
                                                                        .onFailure().recoverWithUni(failure -> {
                                                                                log.error("Failed to cancel workflow run: {}",
                                                                                                runId, failure);
                                                                                // Even if event store fails, still
                                                                                // update the run status
                                                                                return runRepository.update(run);
                                                                        })
                                                                        .onItem()
                                                                        .transformToUni(v -> sagaCoordinator
                                                                                        .compensateCancelledRun(run))
                                                                        .onFailure()
                                                                        .invoke(failure -> log.error(
                                                                                        "Failed to compensate cancelled run: {}",
                                                                                        runId, failure))
                                                                        .onItem().invoke(v -> {
                                                                                cacheManager.invalidate("workflow-run",
                                                                                                runId);
                                                                                activeRunsCache.remove(runId);
                                                                                publishClusterEvent(
                                                                                                new ClusterEvent.RunCancelled(
                                                                                                                runId));
                                                                                logProvenance(runId, "RUN_CANCELLED",
                                                                                                Map.of("reason", reason));
                                                                        });
                                                })
                                                .eventually(() -> releaseLock(lock)));
        }

        // ========================================================================
        // QUERY SIDE
        // ========================================================================

        @CacheResult(cacheName = "workflow-run")
        public Uni<WorkflowRun> getRun(String runId) {
                WorkflowRun cached = activeRunsCache.get(runId);
                if (cached != null) {
                        return Uni.createFrom().item(cached);
                }
                return runRepository.findById(runId)
                                .onItem().ifNull().failWith(() -> new RunNotFoundException(runId))
                                .onItem().invoke(run -> {
                                        if (run.getStatus() == RunStatus.RUNNING
                                                        || run.getStatus() == RunStatus.PENDING) { // Approximated
                                                                                                   // WAITING using
                                                                                                   // PENDING or just
                                                                                                   // RUNNING
                                                activeRunsCache.put(runId, run);
                                        }
                                });
        }

        public Uni<tech.kayys.wayang.workflow.model.WorkflowRunQuery.Result> queryRuns(
                        String tenantId, String workflowId, RunStatus status, int page, int size) {
                return runRepository.query(new WorkflowRunQuery(tenantId, workflowId, status, page, size));
        }

        public Uni<List<WorkflowRun>> getActiveRuns(String tenantId) {
                return runRepository.findActiveByTenant(tenantId);
        }

        public Uni<Long> getActiveRunsCount(String tenantId) {
                if (tenantId == null || tenantId.trim().isEmpty()) {
                        return Uni.createFrom()
                                        .failure(new IllegalArgumentException("Tenant ID cannot be null or empty"));
                }

                return runRepository.countActiveByTenant(tenantId);
        }

        public Multi<WorkflowEvent> streamRunEvents(String runId) {
                return eventStore.streamEvents(runId);
        }

        public Uni<WorkflowRun> rebuildFromEvents(String runId) {
                log.info("Rebuilding run {} from event store", runId);
                return eventStore.getEvents(runId)
                                .onItem().transform(events -> {
                                        if (events.isEmpty()) {
                                                throw new RunNotFoundException(runId);
                                        }
                                        WorkflowEvent firstEvent = events.get(0);
                                        if (firstEvent.type() != WorkflowEventType.CREATED) {
                                                throw new IllegalStateException("First event must be CREATED");
                                        }
                                        WorkflowRun run = WorkflowRun.builder()
                                                        .runId(runId)
                                                        .workflowId(firstEvent.data().get("workflowId").toString())
                                                        .tenantId(firstEvent.data().get("tenantId").toString())
                                                        .triggeredBy(firstEvent.data().get("triggeredBy").toString())
                                                        .build();
                                        for (WorkflowEvent event : events) {
                                                applyEvent(run, event);
                                        }
                                        return run;
                                });
        }

        private void applyEvent(WorkflowRun run, WorkflowEvent event) {
                switch (event.type()) {
                        case CREATED -> {
                        }
                        case STATUS_CHANGED -> {
                                RunStatus newStatus = RunStatus.valueOf(event.data().get("newStatus").toString());
                                run.setStatus(newStatus);
                                if (newStatus == RunStatus.SUCCEEDED && event.data().containsKey("outputs")) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> outputs = (Map<String, Object>) event.data().get("outputs");
                                        run.setOutputs(outputs);
                                }
                        }
                        case NODE_EXECUTED -> {
                                String nodeId = event.data().get("nodeId").toString();
                                NodeExecutionState.NodeStatus status = NodeExecutionState.NodeStatus.valueOf(
                                                event.data().get("status").toString());
                                @SuppressWarnings("unchecked")
                                Map<String, Object> outputs = (Map<String, Object>) event.data().getOrDefault("outputs",
                                                Map.of());
                                String errorMessage = (String) event.data().getOrDefault("errorMessage", "");
                                @SuppressWarnings("unchecked")
                                Map<String, Object> inputs = (Map<String, Object>) event.data().getOrDefault("inputs",
                                                Map.of());
                                Instant startedAt = event.data().containsKey("startedAt")
                                                && !event.data().get("startedAt").toString().isEmpty()
                                                                ? Instant.parse(event.data().get("startedAt")
                                                                                .toString())
                                                                : Instant.now();
                                Instant completedAt = event.data().containsKey("completedAt")
                                                && !event.data().get("completedAt").toString().isEmpty()
                                                                ? Instant.parse(event.data().get("completedAt")
                                                                                .toString())
                                                                : Instant.now();

                                NodeExecutionState nodeState = new NodeExecutionState(
                                                nodeId, status, inputs, outputs, errorMessage, startedAt, completedAt);
                                run.recordNodeState(nodeId, nodeState);
                        }
                        case STATE_UPDATED -> {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> updates = (Map<String, Object>) event.data().get("stateUpdates");
                                run.updateWorkflowState(updates);
                        }
                        case RESUMED -> run.setStatus(RunStatus.RUNNING);
                        case CANCELLED -> run.setStatus(RunStatus.CANCELLED);
                }
        }

        @Transactional
        public Uni<Void> createSnapshot(String runId) {
                return getRun(runId)
                                .onItem().transformToUni(run -> eventStore.getEventCount(runId)
                                                .onItem().transformToUni(eventCount -> snapshotStore.save(
                                                                new WorkflowSnapshot(runId, run, eventCount,
                                                                                Instant.now()))));
        }

        public Uni<WorkflowRun> restoreFromSnapshot(String runId) {
                return snapshotStore.getLatest(runId)
                                .onItem().transformToUni(snapshot -> {
                                        if (snapshot == null)
                                                return rebuildFromEvents(runId);
                                        return eventStore.getEventsAfter(runId, snapshot.eventCount())
                                                        .onItem().transform(recentEvents -> {
                                                                WorkflowRun run = snapshot.state();
                                                                for (WorkflowEvent event : recentEvents) {
                                                                        applyEvent(run, event);
                                                                }
                                                                return run;
                                                        });
                                });
        }

        @Scheduled(every = "1h")
        void autoSnapshot() {
                log.info("Running auto-snapshot");
                runRepository.findAllActive()
                                .onItem().transformToMulti(runs -> Multi.createFrom().iterable(runs))
                                .onItem()
                                .transformToUniAndMerge(
                                                run -> createSnapshot(run.getRunId()).onFailure().recoverWithNull())
                                .subscribe().with(v -> {
                                }, error -> log.error("Auto-snapshot failed", error));
        }

        private void publishClusterEvent(ClusterEvent event) {
                eventBus.publish(EVENT_BUS_ADDRESS, event);
        }

        @ConsumeEvent(EVENT_BUS_ADDRESS)
        void handleClusterEvent(ClusterEvent event) {
                log.debug("Received cluster event: {}", event.getClass().getSimpleName());

                if (event instanceof ClusterEvent.RunCreated e) {
                        cacheManager.invalidate("workflow-run", e.runId());
                        // Optionally add to active runs cache if needed
                } else if (event instanceof ClusterEvent.StatusChanged e) {
                        cacheManager.invalidate("workflow-run", e.runId());
                        activeRunsCache.remove(e.runId());
                } else if (event instanceof ClusterEvent.NodeExecuted e) {
                        cacheManager.invalidate("workflow-run", e.runId());
                } else if (event instanceof ClusterEvent.StateUpdated e) {
                        cacheManager.invalidate("workflow-run", e.runId());
                } else if (event instanceof ClusterEvent.RunResumed e) {
                        cacheManager.invalidate("workflow-run", e.runId());
                } else if (event instanceof ClusterEvent.RunCancelled e) {
                        cacheManager.invalidate("workflow-run", e.runId());
                        activeRunsCache.remove(e.runId());
                } else if (event instanceof ClusterEvent.RunCompleted e) {
                        cacheManager.invalidate("workflow-run", e.runId());
                        activeRunsCache.remove(e.runId());
                }
        }

        @Scheduled(every = "30m")
        @Transactional
        void cleanupStaleRuns() {
                log.info("Cleaning up stale runs");
                Instant threshold = Instant.now().minus(STALE_RUN_THRESHOLD);
                runRepository.findStaleRuns(threshold)
                                .onItem().transformToMulti(runs -> Multi.createFrom().iterable(runs))
                                .onItem()
                                .transformToUniAndMerge(run -> cancelRun(run.getRunId(), run.getTenantId(), "Timeout")
                                                .onFailure().recoverWithNull())
                                .subscribe().with(v -> {
                                }, error -> log.error("Cleanup failed", error));
        }

        private Uni<DistributedLock> acquireLock(String runId, String operation) {
                String lockKey = "workflow-run:" + runId + ":" + operation;
                return lockManager.acquire(lockKey, Duration.ofSeconds(30));
        }

        private Uni<Void> releaseLock(DistributedLock lock) {
                return lockManager.release(lock);
        }

        private void validateStateTransition(RunStatus from, RunStatus to) {
                if (from == to) {
                        return; // No transition needed
                }

                // Check if from state is terminal
                if (isTerminalState(from)) {
                        throw new IllegalStateException(
                                        String.format("Cannot transition from terminal state %s to %s", from, to));
                }

                // Define valid state transitions
                boolean isValidTransition = switch (from) {
                        case PENDING ->
                                to == RunStatus.RUNNING ||
                                                to == RunStatus.CANCELLED ||
                                                to == RunStatus.TIMED_OUT;
                        case RUNNING ->
                                to == RunStatus.SUCCEEDED ||
                                                to == RunStatus.FAILED ||
                                                to == RunStatus.SUSPENDED ||
                                                to == RunStatus.CANCELLED ||
                                                to == RunStatus.PAUSED;
                        case SUSPENDED ->
                                to == RunStatus.RUNNING ||
                                                to == RunStatus.CANCELLED;
                        case PAUSED ->
                                to == RunStatus.RUNNING ||
                                                to == RunStatus.CANCELLED;
                        default -> false;
                };

                if (!isValidTransition) {
                        throw new IllegalStateException(
                                        String.format("Invalid state transition from %s to %s", from, to));
                }
        }

        private boolean isTerminalState(RunStatus status) {
                return switch (status) {
                        case SUCCEEDED, FAILED, CANCELLED, TIMED_OUT -> true;
                        default -> false;
                };
        }

        private void logProvenance(String runId, String event, Map<String, Object> metadata) {
                // Instead of using the deprecated AuditPayload builder approach,
                // use the provenanceService's existing methods that are already working
                // For now, we'll log a debug message
                log.debug("Provenance event: {} for run: {}, metadata: {}", event, runId, metadata);
        }
}
