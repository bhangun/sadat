package tech.kayys.wayang.workflow.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.OptimisticLockException;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.workflow.api.model.RunStatus;
import tech.kayys.wayang.workflow.model.WorkflowRunQuery;
import tech.kayys.wayang.workflow.exception.RunNotFoundException;

import java.time.Instant;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WorkflowRunRepository - Complete reactive repository implementation
 */
@ApplicationScoped
public class WorkflowRunRepository implements PanacheRepositoryBase<WorkflowRun, String> {

        /**
         * Save new workflow run
         */
        public Uni<WorkflowRun> save(WorkflowRun run) {
                return persist(run);
        }

        /**
         * Update workflow run
         */
        public Uni<WorkflowRun> update(WorkflowRun run) {
                return findById(run.getRunId())
                                .onItem().ifNull().failWith(() -> new RunNotFoundException(run.getRunId()))
                                .onItem().transformToUni(entity -> {
                                        return getSession().flatMap(session -> session.merge(run));
                                });
        }

        /**
         * Update with optimistic locking check
         */
        public Uni<WorkflowRun> updateWithOptimisticLock(WorkflowRun run) {
                return getSession().flatMap(session -> session.merge(run))
                                .onFailure(OptimisticLockException.class)
                                .recoverWithUni(th -> {
                                        return Uni.createFrom().failure(
                                                        new ConcurrentModificationException(
                                                                        "Run was modified by another transaction"));
                                });
        }

        /**
         * Query with filters
         */
        public Uni<WorkflowRunQuery.Result> query(WorkflowRunQuery query) {
                StringBuilder hql = new StringBuilder("FROM WorkflowRun e WHERE 1=1");
                Map<String, Object> params = new HashMap<>();

                if (query.tenantId() != null) {
                        hql.append(" AND e.tenantId = :tenantId");
                        params.put("tenantId", query.tenantId());
                }

                if (query.workflowId() != null) {
                        hql.append(" AND e.workflowId = :workflowId");
                        params.put("workflowId", query.workflowId());
                }

                if (query.status() != null) {
                        hql.append(" AND e.status = :status");
                        params.put("status", query.status());
                }

                // Count total
                Uni<Long> countUni = count(
                                hql.toString().replace("FROM WorkflowRun e", ""),
                                params);

                // Get page
                Uni<List<WorkflowRun>> pageUni = find(
                                hql.toString(),
                                Sort.descending("createdAt"),
                                params)
                                .page(Page.of(query.page(), query.size()))
                                .list();

                return Uni.combine().all().unis(countUni, pageUni)
                                .with((total, runs) -> {
                                        int totalPages = (int) Math.ceil((double) total / query.size());
                                        boolean hasNext = (query.page() + 1) < totalPages;
                                        return new WorkflowRunQuery.Result(runs, total, totalPages, hasNext);
                                });
        }

        /**
         * Find active runs by tenant
         */
        public Uni<List<WorkflowRun>> findActiveByTenant(String tenantId) {
                return find(
                                "tenantId = ?1 AND (status = ?2 OR status = ?3)",
                                Sort.descending("createdAt"),
                                tenantId,
                                RunStatus.RUNNING,
                                RunStatus.PENDING) // Assuming PENDING/RUNNING are active
                                .list();
        }

        /**
         * Count active runs by tenant
         */
        public Uni<Long> countActiveByTenant(String tenantId) {
                return count("tenantId = ?1 AND (status = ?2 OR status = ?3)",
                                tenantId, RunStatus.RUNNING, RunStatus.PENDING);
        }

        /**
         * Find all active runs (for snapshot)
         */
        public Uni<List<WorkflowRun>> findAllActive() {
                return find(
                                "status = ?1 OR status = ?2",
                                RunStatus.RUNNING,
                                RunStatus.PENDING)
                                .list();
        }

        /**
         * Find stale runs (for cleanup)
         */
        public Uni<List<WorkflowRun>> findStaleRuns(Instant threshold) {
                return find(
                                "status = ?1 AND updatedAt < ?2",
                                RunStatus.RUNNING,
                                threshold)
                                .list();
        }

        /**
         * Delete old completed runs (cleanup)
         */
        public Uni<Long> deleteOldRuns(Instant before) {
                return delete(
                                "(status = ?1 OR status = ?2 OR status = ?3) AND completedAt < ?4",
                                RunStatus.SUCCEEDED,
                                RunStatus.FAILED,
                                RunStatus.CANCELLED,
                                before);
        }

        // Additional methods from previous file

        public Uni<WorkflowRun> findByIdAndTenant(String runId, String tenantId) {
                return find("runId = ?1 and tenantId = ?2", runId, tenantId).firstResult();
        }

        public Uni<List<WorkflowRun>> findByWorkflowId(String workflowId, String tenantId, int page, int size) {
                return find("workflowId = ?1 and tenantId = ?2", Sort.descending("createdAt"), workflowId, tenantId)
                                .page(page, size).list();
        }

        public Uni<List<WorkflowRun>> findByStatus(RunStatus status, String tenantId, int limit) {
                return find("status = ?1 and tenantId = ?2", Sort.descending("createdAt"), status, tenantId)
                                .page(0, limit).list();
        }

        public Uni<List<WorkflowRun>> findActiveRuns(String tenantId) {
                return find("status in (?1, ?2) and tenantId = ?3", Sort.ascending("lastHeartbeatAt"),
                                RunStatus.RUNNING, RunStatus.SUSPENDED, tenantId).list();
        }

        public Uni<List<WorkflowRun>> findStaleRunsByHeartbeat(Instant threshold) {
                return find("status = ?1 and (lastHeartbeatAt < ?2 or lastHeartbeatAt is null)",
                                RunStatus.RUNNING, threshold).list();
        }

        public Uni<List<WorkflowRun>> findRetryableRuns(Instant now) {
                return find("status = ?1 and attemptNumber < maxAttempts and nextRetryAt is not null and nextRetryAt <= ?2",
                                RunStatus.FAILED, now).list();
        }

        public Uni<Long> deleteOldRuns(Instant olderThan, String tenantId) {
                return delete("status in (?1, ?2, ?3) and completedAt < ?4 and tenantId = ?5",
                                RunStatus.SUCCEEDED, RunStatus.FAILED, RunStatus.CANCELLED, olderThan, tenantId);
        }

        public Uni<Map<String, Long>> getExecutionStats(String tenantId) {
                return getSession().flatMap(session -> session.createQuery(
                                "SELECT r.status, COUNT(r) FROM WorkflowRun r WHERE r.tenantId = :tenantId GROUP BY r.status",
                                Object[].class)
                                .setParameter("tenantId", tenantId)
                                .getResultList()
                                .map(results -> {
                                        Map<String, Long> stats = new HashMap<>();
                                        for (Object[] row : results) {
                                                stats.put(row[0].toString(), (Long) row[1]);
                                        }
                                        return stats;
                                }));
        }
}