package tech.kayys.wayang.workflow.repository;

import java.time.Instant;
import java.util.List;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.workflow.domain.WorkflowExecutionEntity;
import tech.kayys.wayang.workflow.service.WorkflowRuntimeEngine;

/**
 * Workflow Execution Repository
 */
@ApplicationScoped
public class WorkflowExecutionRepository implements PanacheRepository<WorkflowExecutionEntity> {

    private static final Logger LOG = Logger.getLogger(WorkflowExecutionRepository.class);

    @Inject
    ObjectMapper objectMapper;

    /**
     * Save execution start
     */
    public Uni<Void> saveExecutionStart(String executionId, String agentId,
            String workflowId, java.util.Map<String, Object> input) {
        return Uni.createFrom().item(() -> {
            try {
                WorkflowExecutionEntity entity = new WorkflowExecutionEntity();
                entity.executionId = executionId;
                entity.agentId = agentId;
                entity.workflowId = workflowId;
                entity.status = "running";
                entity.inputJson = objectMapper.writeValueAsString(input);
                entity.startTime = Instant.now();
                entity.triggeredBy = "api"; // TODO: Get from context

                return entity;
            } catch (Exception e) {
                throw new RuntimeException("Failed to save execution", e);
            }
        })
                .chain(entity -> entity.persistAndFlush())
                .replaceWithVoid();
    }

    /**
     * Save execution completion
     */
    public Uni<Void> saveExecutionComplete(String executionId,
            WorkflowRuntimeEngine.ExecutionResult result) {

        return WorkflowExecutionEntity.<WorkflowExecutionEntity>findById(executionId)
                .chain(entity -> {
                    if (entity == null) {
                        LOG.warnf("Execution not found: %s", executionId);
                        return Uni.createFrom().voidItem();
                    }

                    try {
                        entity.status = result.isSuccess() ? "completed" : "failed";
                        entity.outputJson = objectMapper.writeValueAsString(result.getOutput());
                        entity.traceJson = objectMapper.writeValueAsString(result.getTrace());
                        entity.errorMessage = result.getError();
                        entity.endTime = Instant.now();
                        entity.durationMs = entity.endTime.toEpochMilli() - entity.startTime.toEpochMilli();

                        return entity.persistAndFlush().replaceWithVoid();
                    } catch (Exception e) {
                        LOG.errorf(e, "Failed to save execution completion: %s", executionId);
                        return Uni.createFrom().failure(e);
                    }
                })
                .replaceWithVoid();
    }

    /**
     * Get execution history for agent
     */
    public Uni<List<WorkflowExecutionEntity>> getExecutionHistory(
            String agentId, int page, int size) {

        return WorkflowExecutionEntity.<WorkflowExecutionEntity>find(
                "agentId = ?1 ORDER BY startTime DESC",
                agentId)
                .page(page, size)
                .list();
    }

    /**
     * Get execution by ID
     */
    public Uni<WorkflowExecutionEntity> getExecution(String executionId) {
        return WorkflowExecutionEntity.findById(executionId);
    }

    /**
     * Get running executions
     */
    public Uni<List<WorkflowExecutionEntity>> getRunningExecutions() {
        return WorkflowExecutionEntity.<WorkflowExecutionEntity>find(
                "status = ?1",
                "running").list();
    }

    /**
     * Get execution statistics
     */
    public Uni<ExecutionStats> getExecutionStats(String agentId, Instant since) {
        // Use native query for better performance
        String query = "SELECT " +
                "COUNT(*) as total, " +
                "COUNT(CASE WHEN status = 'completed' THEN 1 END) as completed, " +
                "COUNT(CASE WHEN status = 'failed' THEN 1 END) as failed, " +
                "AVG(durationMs) as avgDuration " +
                "FROM workflow_executions " +
                "WHERE agentId = ?1 AND startTime >= ?2";

        return Panache.getSession().flatMap(session -> session.createNativeQuery(query)
                .setParameter(1, agentId)
                .setParameter(2, since)
                .getSingleResult()
                .map(result -> {
                    Object[] row = (Object[]) result;
                    ExecutionStats stats = new ExecutionStats();
                    stats.total = ((Number) row[0]).longValue();
                    stats.completed = ((Number) row[1]).longValue();
                    stats.failed = ((Number) row[2]).longValue();
                    stats.avgDuration = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
                    return stats;
                }));
    }

    public static class ExecutionStats {
        public long total;
        public long completed;
        public long failed;
        public double avgDuration;
    }
}