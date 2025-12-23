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
import io.quarkus.hibernate.reactive.panache.Panache;
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
                // Validate input
                if (request == null) {
                        return Uni.createFrom()
                                        .failure(new IllegalArgumentException("CreateRunRequest cannot be null"));
                }

                String tenantId = "default-tenant";
                log.info("Creating workflow run for workflow: {}, tenant: {}", request.getWorkflowId(), tenantId);

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

                WorkflowEvent createdEvent = WorkflowEvent.created(run.getRunId(),
                                Map.of(
                                                "workflowId", request.getWorkflowId(),
                                                "workflowVersion",
                                                request.getWorkflowVersion() != null ? request.getWorkflowVersion()
                                                                : "1.0.0",
                                                "tenantId", tenantId,
                                                "triggeredBy", "manual",
                                                "inputs",
                                                request.getInputs() != null ? request.getInputs() : Map.of()));

                return eventStore.append(run.getRunId(), createdEvent)
                                .onItem().transformToUni(eventId -> runRepository.save(run))
                                .onFailure().invoke(failure -> {
                                        log.error("Failed to create workflow run for workflow: {}",
                                                        request.getWorkflowId(), failure);
                                        // Clear any cache that might have been affected
                                        activeRunsCache.remove(run.getRunId());
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
                                .flatMap(lock -> runRepository.findById(runId)
                                                .onItem().ifNull().failWith(() -> new RunNotFoundException(runId))
                                                .onItem().invoke(run -> {
                                                        if (!run.getTenantId().equals(tenantId)) {
                                                                throw new IllegalStateException(
                                                                                "Tenant mismatch for run: " + runId);
                                                        }
                                                })
                                                .flatMap(run -> {
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
                                .flatMap(lock -> runRepository.findById(runId)
                                                .onItem().ifNull().failWith(() -> new RunNotFoundException(runId))
                                                .onItem().invoke(run -> {
                                                        if (!run.getTenantId().equals(tenantId)) {
                                                                throw new IllegalStateException(
                                                                                "Tenant mismatch for run: " + runId);
                                                        }
                                                })
                                                .flatMap(run -> {
                                                        WorkflowEvent nodeEvent = WorkflowEvent.nodeExecuted(
                                                                        runId,
                                                                        nodeState.nodeId(),
                                                                        nodeState.status().toString());

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
                                .flatMap(lock -> runRepository.findById(runId)
                                                .onItem().ifNull().failWith(() -> new RunNotFoundException(runId))
                                                .onItem().invoke(run -> {
                                                        if (!run.getTenantId().equals(tenantId)) {
                                                                throw new IllegalStateException(
                                                                                "Tenant mismatch for run: " + runId);
                                                        }
                                                        // Validate state transition
                                                        validateStateTransition(run.getStatus(), RunStatus.SUCCEEDED);
                                                })
                                                .flatMap(run -> {
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
                                .flatMap(lock -> runRepository.findById(runId)
                                                .onItem().ifNull().failWith(() -> new RunNotFoundException(runId))
                                                .onItem().invoke(run -> {
                                                        if (!run.getTenantId().equals(tenantId)) {
                                                                throw new IllegalStateException(
                                                                                "Tenant mismatch for run: " + runId);
                                                        }
                                                        // Validate state transition
                                                        validateStateTransition(run.getStatus(), RunStatus.FAILED);
                                                })
                                                .flatMap(run -> {
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
                Map<String, Object> safeStateUpdates = (stateUpdates != null) ? stateUpdates
                                : java.util.Collections.emptyMap();

                return acquireLock(runId, "state-update")
                                .flatMap(lock -> runRepository.findById(runId)
                                                .onItem().ifNull().failWith(() -> new RunNotFoundException(runId))
                                                .onItem().invoke(run -> {
                                                        if (!run.getTenantId().equals(tenantId)) {
                                                                throw new IllegalStateException(
                                                                                "Tenant mismatch for run: " + runId);
                                                        }
                                                })
                                                .flatMap(run -> {
                                                        WorkflowEvent stateEvent = WorkflowEvent.stateUpdated(runId,
                                                                        safeStateUpdates);

                                                        return eventStore.append(runId, stateEvent)
                                                                        .onItem().transformToUni(eventId -> {
                                                                                run.updateWorkflowState(
                                                                                                safeStateUpdates);
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
                                                                                                                safeStateUpdates));
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
                                .flatMap(lock -> runRepository.findById(runId)
                                                .onItem().ifNull().failWith(() -> new RunNotFoundException(runId))
                                                .onItem().invoke(run -> {
                                                        if (!run.getTenantId().equals(tenantId)) {
                                                                throw new IllegalStateException(
                                                                                "Tenant mismatch for run: " + runId);
                                                        }
                                                })
                                                .flatMap(run -> {
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
                                .flatMap(lock -> runRepository.findById(runId)
                                                .onItem().ifNull().failWith(() -> new RunNotFoundException(runId))
                                                .onItem().invoke(run -> {
                                                        if (!run.getTenantId().equals(tenantId)) {
                                                                throw new IllegalStateException(
                                                                                "Tenant mismatch for run: " + runId);
                                                        }
                                                })
                                                .flatMap(run -> {
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
                if (runId == null || runId.trim().isEmpty()) {
                        return Uni.createFrom().failure(new IllegalArgumentException("Run ID cannot be null or empty"));
                }

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
                                })
                                .onFailure().invoke(throwable -> {
                                        log.error("Error retrieving workflow run: {}", runId, throwable);
                                });
        }

        public Uni<tech.kayys.wayang.workflow.model.WorkflowRunQuery.Result> queryRuns(
                        String tenantId, String workflowId, RunStatus status, int page, int size) {
                if (tenantId == null || tenantId.trim().isEmpty()) {
                        return Uni.createFrom()
                                        .failure(new IllegalArgumentException("Tenant ID cannot be null or empty"));
                }

                // Normalize page and size values
                int normalizedPage = Math.max(0, page);
                int normalizedSize = Math.min(Math.max(1, size), 100); // Max 100 items per page

                return runRepository
                                .query(new WorkflowRunQuery(tenantId, workflowId, status, normalizedPage,
                                                normalizedSize))
                                .onFailure().invoke(throwable -> {
                                        log.error("Error querying workflow runs for tenant: {}", tenantId, throwable);
                                });
        }

        public Uni<List<WorkflowRun>> getActiveRuns(String tenantId) {
                if (tenantId == null || tenantId.trim().isEmpty()) {
                        return Uni.createFrom()
                                        .failure(new IllegalArgumentException("Tenant ID cannot be null or empty"));
                }

                return runRepository.findActiveByTenant(tenantId)
                                .onFailure().invoke(throwable -> {
                                        log.error("Error retrieving active runs for tenant: {}", tenantId, throwable);
                                });
        }

        public Uni<Long> getActiveRunsCount(String tenantId) {
                if (tenantId == null || tenantId.trim().isEmpty()) {
                        return Uni.createFrom()
                                        .failure(new IllegalArgumentException("Tenant ID cannot be null or empty"));
                }

                return runRepository.countActiveByTenant(tenantId);
        }

        public Multi<WorkflowEvent> streamRunEvents(String runId) {
                if (runId == null || runId.trim().isEmpty()) {
                        throw new IllegalArgumentException("Run ID cannot be null or empty");
                }
                return eventStore.streamEvents(runId)
                                .onFailure().invoke(throwable -> {
                                        log.error("Error streaming events for workflow run: {}", runId, throwable);
                                });
        }

        public Uni<WorkflowRun> rebuildFromEvents(String runId) {
                if (runId == null || runId.trim().isEmpty()) {
                        return Uni.createFrom().failure(new IllegalArgumentException("Run ID cannot be null or empty"));
                }

                log.info("Rebuilding run {} from event store", runId);
                return eventStore.getEvents(runId)
                                .onItem().transform(events -> {
                                        if (events == null || events.isEmpty()) {
                                                throw new RunNotFoundException(runId);
                                        }
                                        WorkflowEvent firstEvent = events.get(0);
                                        if (firstEvent.type() != WorkflowEventType.CREATED) {
                                                throw new IllegalStateException(
                                                                "First event must be CREATED for run: " + runId);
                                        }

                                        var eventData = firstEvent.data();
                                        if (!eventData.containsKey("workflowId") ||
                                                        !eventData.containsKey("tenantId") ||
                                                        !eventData.containsKey("triggeredBy")) {
                                                throw new IllegalStateException(
                                                                "Missing required data in CREATED event for run: "
                                                                                + runId);
                                        }

                                        WorkflowRun run = WorkflowRun.builder()
                                                        .runId(runId)
                                                        .workflowId(eventData.get("workflowId").toString())
                                                        .tenantId(eventData.get("tenantId").toString())
                                                        .triggeredBy(eventData.get("triggeredBy").toString())
                                                        .build();
                                        for (WorkflowEvent event : events) {
                                                applyEvent(run, event);
                                        }
                                        return run;
                                })
                                .onFailure().invoke(throwable -> {
                                        log.error("Error rebuilding workflow run from events: {}", runId, throwable);
                                });
        }

        private void applyEvent(WorkflowRun run, WorkflowEvent event) {
                if (run == null || event == null) {
                        log.warn("Cannot apply null event to workflow run");
                        return;
                }

                var eventData = event.data();
                if (eventData == null) {
                        log.warn("Event data is null for event type: {}", event.type());
                        return;
                }

                switch (event.type()) {
                        case CREATED -> {
                                // Nothing to do here - run is already initialized during rebuild
                        }
                        case STATUS_CHANGED -> {
                                try {
                                        Object newStatusObj = eventData.get("newStatus");
                                        if (newStatusObj == null) {
                                                log.warn("Missing newStatus in STATUS_CHANGED event");
                                                return;
                                        }
                                        RunStatus newStatus = RunStatus.valueOf(newStatusObj.toString());
                                        run.setStatus(newStatus);
                                        if (newStatus == RunStatus.SUCCEEDED && eventData.containsKey("outputs")) {
                                                @SuppressWarnings("unchecked")
                                                Map<String, Object> outputs = (Map<String, Object>) eventData
                                                                .get("outputs");
                                                run.setOutputs(outputs);
                                        }
                                } catch (IllegalArgumentException e) {
                                        log.warn("Invalid status in STATUS_CHANGED event: {}",
                                                        eventData.get("newStatus"), e);
                                }
                        }
                        case NODE_EXECUTED -> {
                                Object nodeIdObj = eventData.get("nodeId");
                                if (nodeIdObj == null) {
                                        log.warn("Missing nodeId in NODE_EXECUTED event");
                                        return;
                                }
                                String nodeId = nodeIdObj.toString();

                                Object statusObj = eventData.get("status");
                                if (statusObj == null) {
                                        log.warn("Missing status in NODE_EXECUTED event for node: {}", nodeId);
                                        return;
                                }

                                try {
                                        NodeExecutionState.NodeStatus status = NodeExecutionState.NodeStatus.valueOf(
                                                        statusObj.toString());
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> outputs = (Map<String, Object>) eventData.getOrDefault(
                                                        "outputs",
                                                        Map.of());
                                        String errorMessage = (String) eventData.getOrDefault("errorMessage", "");
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> inputs = (Map<String, Object>) eventData.getOrDefault(
                                                        "inputs",
                                                        Map.of());
                                        Instant startedAt = eventData.containsKey("startedAt")
                                                        && eventData.get("startedAt") != null
                                                                        ? Instant.parse(eventData.get("startedAt")
                                                                                        .toString())
                                                                        : Instant.now();
                                        Instant completedAt = eventData.containsKey("completedAt")
                                                        && eventData.get("completedAt") != null
                                                                        ? Instant.parse(eventData.get("completedAt")
                                                                                        .toString())
                                                                        : Instant.now();

                                        NodeExecutionState nodeState = new NodeExecutionState(
                                                        nodeId, status, inputs, outputs, errorMessage, startedAt,
                                                        completedAt);
                                        run.recordNodeState(nodeId, nodeState);
                                } catch (IllegalArgumentException e) {
                                        log.warn("Invalid status in NODE_EXECUTED event for node {}: {}", nodeId,
                                                        statusObj, e);
                                } catch (Exception e) {
                                        log.warn("Error processing NODE_EXECUTED event for node: {}", nodeId, e);
                                }
                        }
                        case STATE_UPDATED -> {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> updates = (Map<String, Object>) eventData.get("stateUpdates");
                                if (updates != null) {
                                        run.updateWorkflowState(updates);
                                }
                        }
                        case RESUMED -> run.setStatus(RunStatus.RUNNING);
                        case CANCELLED -> run.setStatus(RunStatus.CANCELLED);
                        default -> log.warn("Unknown event type: {}", event.type());
                }
        }

        @Transactional
        public Uni<Void> createSnapshot(String runId) {
                if (runId == null || runId.trim().isEmpty()) {
                        return Uni.createFrom().failure(new IllegalArgumentException("Run ID cannot be null or empty"));
                }

                return getRun(runId)
                                .flatMap(run -> eventStore.getEventCount(runId)
                                                .onItem().transformToUni(eventCount -> snapshotStore.save(
                                                                new WorkflowSnapshot(runId, run, eventCount,
                                                                                Instant.now()))))
                                .onFailure().invoke(throwable -> {
                                        log.error("Error creating snapshot for workflow run: {}", runId, throwable);
                                });
        }

        public Uni<WorkflowRun> restoreFromSnapshot(String runId) {
                return snapshotStore.getLatest(runId)
                                .flatMap(snapshot -> {
                                        if (snapshot == null) {
                                                return rebuildFromEvents(runId);
                                        }

                                        return eventStore.getEventsAfter(runId, snapshot.eventCount())
                                                        .onItem().transform(recentEvents -> {
                                                                WorkflowRun run = snapshot.workflowRun();
                                                                for (WorkflowEvent event : recentEvents) {
                                                                        applyEvent(run, event);
                                                                }
                                                                return run;
                                                        })
                                                        .onFailure().recoverWithUni(throwable -> {
                                                                log.error("Failed to restore from snapshot for run: {}",
                                                                                runId, throwable);
                                                                return rebuildFromEvents(runId);
                                                        });
                                })
                                .onFailure().recoverWithUni(throwable -> {
                                        log.error("Failed to retrieve snapshot for run: {}", runId, throwable);
                                        return rebuildFromEvents(runId);
                                });
        }

        @Scheduled(every = "1h")
        Uni<Void> autoSnapshot() {
                log.info("Running auto-snapshot");
                return Panache.withTransaction(() -> runRepository.findAllActive()
                                .onItem().transformToMulti(runs -> Multi.createFrom().iterable(runs))
                                .onItem().transformToUniAndMerge(run -> {
                                        if (run != null) {
                                                return createSnapshot(run.getRunId())
                                                                .onFailure()
                                                                .invoke(error -> log.warn(
                                                                                "Failed to create snapshot for run: {}",
                                                                                run.getRunId(), error))
                                                                .onItem()
                                                                .invoke(v -> log.debug("Created snapshot for run: {}",
                                                                                run.getRunId()));
                                        }
                                        return Uni.createFrom().voidItem();
                                })
                                .collect().asList()
                                .replaceWithVoid())
                                .onFailure().invoke(error -> log.error("Auto-snapshot failed", error));
        }

        private void publishClusterEvent(ClusterEvent event) {
                eventBus.publish(EVENT_BUS_ADDRESS, event);
        }

        @ConsumeEvent(EVENT_BUS_ADDRESS)
        void handleClusterEvent(ClusterEvent event) {
                if (event == null) {
                        log.warn("Received null cluster event");
                        return;
                }

                log.debug("Received cluster event: {}", event.getClass().getSimpleName());

                try {
                        if (event instanceof ClusterEvent.RunCreated e) {
                                if (e.runId() != null) {
                                        cacheManager.invalidate("workflow-run", e.runId());
                                        // Optionally add to active runs cache if needed
                                }
                        } else if (event instanceof ClusterEvent.StatusChanged e) {
                                if (e.runId() != null) {
                                        cacheManager.invalidate("workflow-run", e.runId());
                                        activeRunsCache.remove(e.runId());
                                }
                        } else if (event instanceof ClusterEvent.NodeExecuted e) {
                                if (e.runId() != null) {
                                        cacheManager.invalidate("workflow-run", e.runId());
                                }
                        } else if (event instanceof ClusterEvent.StateUpdated e) {
                                if (e.runId() != null) {
                                        cacheManager.invalidate("workflow-run", e.runId());
                                }
                        } else if (event instanceof ClusterEvent.RunResumed e) {
                                if (e.runId() != null) {
                                        cacheManager.invalidate("workflow-run", e.runId());
                                }
                        } else if (event instanceof ClusterEvent.RunCancelled e) {
                                if (e.runId() != null) {
                                        cacheManager.invalidate("workflow-run", e.runId());
                                        activeRunsCache.remove(e.runId());
                                }
                        } else if (event instanceof ClusterEvent.RunCompleted e) {
                                if (e.runId() != null) {
                                        cacheManager.invalidate("workflow-run", e.runId());
                                        activeRunsCache.remove(e.runId());
                                }
                        } else {
                                log.debug("Unknown cluster event type: {}", event.getClass().getSimpleName());
                        }
                } catch (Exception ex) {
                        log.error("Error processing cluster event: {}", event.getClass().getSimpleName(), ex);
                }
        }

        @Scheduled(every = "30m")
        Uni<Void> cleanupStaleRuns() {
                log.info("Cleaning up stale runs");
                Instant threshold = Instant.now().minus(STALE_RUN_THRESHOLD);
                return Panache.withTransaction(() -> runRepository.findStaleRuns(threshold)
                                .onItem().transformToMulti(runs -> Multi.createFrom().iterable(runs))
                                .onItem().transformToUniAndMerge(run -> {
                                        if (run != null) {
                                                return cancelRun(run.getRunId(), run.getTenantId(), "Timeout")
                                                                .onFailure()
                                                                .invoke(error -> log.warn(
                                                                                "Failed to cancel stale run: {}",
                                                                                run.getRunId(), error))
                                                                .onItem()
                                                                .invoke(v -> log.info("Cancelled stale run: {}",
                                                                                run.getRunId()))
                                                                .replaceWithVoid();
                                        }
                                        return Uni.createFrom().voidItem();
                                })
                                .collect().asList()
                                .replaceWithVoid())
                                .onFailure().invoke(error -> log.error("Cleanup failed", error));
        }

        private Uni<DistributedLock> acquireLock(String runId, String operation) {
                String lockKey = "workflow-run:" + runId + ":" + operation;
                return lockManager.acquire(lockKey, Duration.ofSeconds(30))
                                .onItem().ifNull()
                                .failWith(() -> new IllegalStateException("Could not acquire lock for operation: "
                                                + operation + " on run: " + runId));
        }

        private Uni<Void> releaseLock(DistributedLock lock) {
                if (lock == null) {
                        return Uni.createFrom().voidItem();
                }
                return lockManager.release(lock)
                                .onFailure()
                                .invoke(throwable -> log.warn("Failed to release lock: {}", lock.key(), throwable));
        }

        private void validateStateTransition(RunStatus from, RunStatus to) {
                if (from == null) {
                        throw new IllegalArgumentException("From state cannot be null");
                }
                if (to == null) {
                        throw new IllegalArgumentException("To state cannot be null");
                }

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
                        case PENDING -> to == RunStatus.RUNNING ||
                                        to == RunStatus.CANCELLED ||
                                        to == RunStatus.TIMED_OUT;
                        case RUNNING -> to == RunStatus.SUCCEEDED ||
                                        to == RunStatus.FAILED ||
                                        to == RunStatus.SUSPENDED ||
                                        to == RunStatus.CANCELLED ||
                                        to == RunStatus.PAUSED;
                        case SUSPENDED -> to == RunStatus.RUNNING ||
                                        to == RunStatus.CANCELLED;
                        case PAUSED -> to == RunStatus.RUNNING ||
                                        to == RunStatus.CANCELLED;
                        default -> false;
                };

                if (!isValidTransition) {
                        throw new IllegalStateException(
                                        String.format("Invalid state transition from %s to %s", from, to));
                }
        }

        private boolean isTerminalState(RunStatus status) {
                if (status == null) {
                        return false; // null is not a terminal state
                }
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
