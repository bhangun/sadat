package tech.kayys.wayang.workflow.scheduler.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.workflow.scheduler.dto.ScheduleExecution;
import tech.kayys.wayang.workflow.scheduler.model.WorkflowSchedule;

import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ScheduleStore: Persistence layer for workflow schedules.
 * 
 * Implementation Notes:
 * - In-memory store for demonstration
 * - Production: Use PostgreSQL with Hibernate Reactive
 * - Implements optimistic locking for concurrent updates
 * - Indexes on: tenantId, workflowId, nextExecutionAt
 */
@ApplicationScoped
public class ScheduleStore {

    private static final Logger LOG = Logger.getLogger(ScheduleStore.class);

    // In-memory stores (replace with database in production)
    private final Map<String, WorkflowSchedule> schedules = new ConcurrentHashMap<>();
    private final Map<String, ScheduleExecution> executions = new ConcurrentHashMap<>();

    /**
     * Save or update a schedule.
     */
    public Uni<WorkflowSchedule> save(WorkflowSchedule schedule) {
        schedules.put(schedule.getScheduleId(), schedule);
        LOG.debugf("Saved schedule %s", schedule.getScheduleId());
        return Uni.createFrom().item(schedule);
    }

    /**
     * Find schedule by ID.
     */
    public Uni<WorkflowSchedule> findById(String scheduleId) {
        WorkflowSchedule schedule = schedules.get(scheduleId);
        return Uni.createFrom().item(schedule);
    }

    /**
     * Find schedules due for execution.
     * Returns schedules where nextExecutionAt <= now and enabled = true.
     */
    public Uni<List<WorkflowSchedule>> findDueSchedules(Instant now) {
        List<WorkflowSchedule> due = schedules.values().stream()
                .filter(s -> s.isEnabled())
                .filter(s -> s.getNextExecutionAt() != null)
                .filter(s -> !s.getNextExecutionAt().isAfter(now))
                .collect(Collectors.toList());

        LOG.debugf("Found %d due schedules", due.size());
        return Uni.createFrom().item(due);
    }

    /**
     * Find schedules by workflow ID.
     */
    public Uni<List<WorkflowSchedule>> findByWorkflowId(String workflowId) {
        List<WorkflowSchedule> result = schedules.values().stream()
                .filter(s -> s.getWorkflowId().equals(workflowId))
                .collect(Collectors.toList());

        return Uni.createFrom().item(result);
    }

    /**
     * Find schedules by tenant ID.
     */
    public Uni<List<WorkflowSchedule>> findByTenantId(String tenantId) {
        List<WorkflowSchedule> result = schedules.values().stream()
                .filter(s -> s.getTenantId().equals(tenantId))
                .collect(Collectors.toList());

        return Uni.createFrom().item(result);
    }

    /**
     * Delete a schedule.
     */
    public Uni<Void> delete(String scheduleId) {
        schedules.remove(scheduleId);
        LOG.infof("Deleted schedule %s", scheduleId);
        return Uni.createFrom().voidItem();
    }

    /**
     * Save schedule execution record.
     */
    public Uni<ScheduleExecution> saveExecution(ScheduleExecution execution) {
        executions.put(execution.getExecutionId(), execution);
        return Uni.createFrom().item(execution);
    }

    /**
     * Find execution history for a schedule.
     */
    public Uni<List<ScheduleExecution>> findExecutions(String scheduleId, int limit) {
        List<ScheduleExecution> result = executions.values().stream()
                .filter(e -> e.getScheduleId().equals(scheduleId))
                .sorted((a, b) -> b.getStartedAt().compareTo(a.getStartedAt()))
                .limit(limit)
                .collect(Collectors.toList());

        return Uni.createFrom().item(result);
    }
}
