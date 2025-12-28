package tech.kayys.wayang.workflow.scheduler.service;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import tech.kayys.wayang.sdk.dto.TriggerWorkflowRequest;
import tech.kayys.wayang.workflow.scheduler.dto.CircuitBreaker;
import tech.kayys.wayang.workflow.scheduler.dto.ExecutionStatus;
import tech.kayys.wayang.workflow.scheduler.dto.MissedExecutionStrategy;
import tech.kayys.wayang.workflow.scheduler.dto.ScheduleExecution;
import tech.kayys.wayang.workflow.scheduler.dto.ScheduleRequest;
import tech.kayys.wayang.workflow.scheduler.dto.ScheduleType;
import tech.kayys.wayang.workflow.scheduler.dto.ScheduleUpdateRequest;
import tech.kayys.wayang.workflow.engine.WorkflowEngine;
import tech.kayys.wayang.workflow.scheduler.model.WorkflowSchedule;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WorkflowScheduler: Enterprise-grade workflow scheduling system.
 * 
 * Features:
 * - Cron-based scheduling with timezone support
 * - Interval-based recurring execution
 * - Calendar-aware scheduling (business days, holidays)
 * - One-time scheduled execution
 * - Missed execution handling with configurable strategies
 * - Schedule versioning and rollback
 * - Multi-tenant isolation
 * - Execution history and audit trail
 * - Dynamic schedule updates without restart
 * - Circuit breaker for failing schedules
 * 
 * Architecture:
 * - Non-blocking reactive scheduling
 * - Distributed locking for multi-instance deployments
 * - Event-driven execution tracking
 * - Graceful degradation on errors
 */
@ApplicationScoped
public class WorkflowScheduler {

    private static final Logger LOG = Logger.getLogger(WorkflowScheduler.class);

    @Inject
    WorkflowEngine engine;

    @Inject
    ScheduleStore scheduleStore;

    @Inject
    CalendarService calendarService;

    @Inject
    ScheduleLockManager lockManager;

    @Inject
    ScheduleMetrics metrics;

    // Active executions tracking
    private final Map<String, ScheduleExecution> activeExecutions = new ConcurrentHashMap<>();

    // Circuit breaker for failing schedules
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    /**
     * Main scheduler - runs every minute to check for due schedules.
     * Uses distributed locking to prevent duplicate execution in clustered
     * environments.
     */
    @Scheduled(every = "60s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void checkSchedules() {
        LOG.debug("Checking scheduled workflows...");

        Instant now = Instant.now();

        scheduleStore.findDueSchedules(now)
                .onItem().transformToMulti(schedules -> io.smallrye.mutiny.Multi.createFrom().iterable(schedules))
                .onItem().transformToUniAndMerge(schedule -> executeScheduleWithLock(schedule))
                .collect().asList()
                .subscribe().with(
                        results -> LOG.debugf("Processed %d schedules", results.size()),
                        error -> LOG.errorf(error, "Failed to check schedules"));
    }

    /**
     * Execute schedule with distributed lock to prevent duplicate execution.
     */
    private Uni<Void> executeScheduleWithLock(WorkflowSchedule schedule) {
        String lockKey = "schedule:" + schedule.getScheduleId();

        return lockManager.acquireLock(lockKey, 60)
                .onItem().transformToUni(acquired -> {
                    if (!acquired) {
                        LOG.debugf("Could not acquire lock for schedule %s",
                                schedule.getScheduleId());
                        return Uni.createFrom().voidItem();
                    }

                    // Check circuit breaker
                    CircuitBreaker breaker = getCircuitBreaker(schedule.getScheduleId());
                    if (breaker.isOpen()) {
                        LOG.warnf("Circuit breaker open for schedule %s, skipping execution",
                                schedule.getScheduleId());
                        return Uni.createFrom().voidItem();
                    }

                    return executeSchedule(schedule)
                            .eventually(() -> lockManager.releaseLock(lockKey));
                });
    }

    /**
     * Execute a scheduled workflow.
     */
    private Uni<Void> executeSchedule(WorkflowSchedule schedule) {
        String executionId = UUID.randomUUID().toString();

        LOG.infof("Executing scheduled workflow %s (schedule: %s, execution: %s)",
                schedule.getWorkflowId(), schedule.getScheduleId(), executionId);

        // Record execution start
        ScheduleExecution execution = ScheduleExecution.builder()
                .executionId(executionId)
                .scheduleId(schedule.getScheduleId())
                .workflowId(schedule.getWorkflowId())
                .tenantId(schedule.getTenantId())
                .scheduledFor(schedule.getNextExecutionAt())
                .startedAt(Instant.now())
                .status(ExecutionStatus.RUNNING)
                .build();

        activeExecutions.put(executionId, execution);
        metrics.recordScheduleExecution(schedule.getScheduleId());

        // Build trigger request
        TriggerWorkflowRequest request = new TriggerWorkflowRequest(
                schedule.getWorkflowId(),
                schedule.getWorkflowVersion(),
                enrichInputs(schedule.getInputs(), execution),
                "schedule:" + schedule.getScheduleId() + ":" + executionId);

        // Trigger workflow
        return engine.triggerWorkflow(request)
                .onItem().transformToUni(response -> {
                    handleExecutionSuccess(execution, response.runId());
                    return updateNextExecution(schedule);
                })
                .onFailure().recoverWithUni(error -> {
                    handleExecutionFailure(execution, error);
                    return handleMissedExecution(schedule, error);
                });
    }

    /**
     * Enrich inputs with execution metadata.
     */
    private Map<String, Object> enrichInputs(
            Map<String, Object> inputs,
            ScheduleExecution execution) {

        Map<String, Object> enriched = new HashMap<>(inputs);
        enriched.put("_schedule", Map.of(
                "executionId", execution.getExecutionId(),
                "scheduleId", execution.getScheduleId(),
                "scheduledFor", execution.getScheduledFor().toString(),
                "startedAt", execution.getStartedAt().toString()));
        return enriched;
    }

    /**
     * Update schedule's next execution time based on schedule type.
     */
    private Uni<Void> updateNextExecution(WorkflowSchedule schedule) {
        Instant nextExecution = calculateNextExecution(schedule);

        WorkflowSchedule updated = schedule.toBuilder()
                .lastExecutedAt(Instant.now())
                .nextExecutionAt(nextExecution)
                .executionCount(schedule.getExecutionCount() + 1)
                .consecutiveFailures(0) // Reset on success
                .build();

        return scheduleStore.save(updated)
                .onItem().invoke(saved -> LOG.debugf("Updated schedule %s, next execution: %s",
                        schedule.getScheduleId(), nextExecution))
                .onFailure().invoke(error -> LOG.errorf(error, "Failed to update schedule %s",
                        schedule.getScheduleId()))
                .replaceWithVoid();
    }

    /**
     * Calculate next execution time based on schedule type and configuration.
     */
    private Instant calculateNextExecution(WorkflowSchedule schedule) {
        Instant now = Instant.now();
        ZoneId timezone = ZoneId.of(schedule.getTimezone());

        return switch (schedule.getScheduleType()) {
            case CRON -> calculateCronNext(
                    schedule.getCronExpression(),
                    timezone);

            case INTERVAL -> now.plusMillis(schedule.getInterval());

            case CALENDAR -> calculateCalendarNext(
                    schedule.getHour(),
                    schedule.getMinute(),
                    timezone);

            case ONE_TIME -> null; // No next execution

            default -> throw new IllegalStateException(
                    "Unknown schedule type: " + schedule.getScheduleType());
        };
    }

    /**
     * Calculate next execution for cron expression.
     * Uses Quartz CronExpression for robust cron parsing.
     */
    private Instant calculateCronNext(String cronExpression, ZoneId timezone) {
        try {
            org.quartz.CronExpression cron = new org.quartz.CronExpression(cronExpression);
            cron.setTimeZone(TimeZone.getTimeZone(timezone));

            Date next = cron.getNextValidTimeAfter(new Date());
            return next != null ? next.toInstant() : null;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to calculate cron next execution: %s",
                    cronExpression);
            // Fallback to next day at same time
            return ZonedDateTime.now(timezone)
                    .plusDays(1)
                    .toInstant();
        }
    }

    /**
     * Calculate next execution for calendar-based schedule.
     * Skips weekends and holidays.
     */
    private Instant calculateCalendarNext(int hour, int minute, ZoneId timezone) {
        ZonedDateTime next = ZonedDateTime.now(timezone)
                .plusDays(1)
                .withHour(hour)
                .withMinute(minute)
                .withSecond(0)
                .withNano(0);

        // Skip non-business days
        while (!calendarService.isBusinessDay(next.toLocalDate())) {
            next = next.plusDays(1);
        }

        return next.toInstant();
    }

    /**
     * Handle missed execution based on strategy.
     */
    private Uni<Void> handleMissedExecution(
            WorkflowSchedule schedule,
            Throwable error) {

        int failures = schedule.getConsecutiveFailures() + 1;

        WorkflowSchedule updated = schedule.toBuilder()
                .consecutiveFailures(failures)
                .lastError(error.getMessage())
                .lastErrorAt(Instant.now())
                .build();

        // Open circuit breaker if too many failures
        if (failures >= 3) {
            CircuitBreaker breaker = getCircuitBreaker(schedule.getScheduleId());
            breaker.open();
            LOG.warnf("Opening circuit breaker for schedule %s after %d failures",
                    schedule.getScheduleId(), failures);
        }

        // Apply missed execution strategy
        return switch (schedule.getMissedExecutionStrategy()) {
            case SKIP -> {
                LOG.infof("Skipping missed execution for schedule %s",
                        schedule.getScheduleId());
                yield updateNextExecution(updated);
            }

            case RUN_IMMEDIATELY -> {
                LOG.infof("Running missed execution immediately for schedule %s",
                        schedule.getScheduleId());
                yield executeSchedule(updated);
            }

            case ALERT_ONLY -> {
                LOG.warnf("Alerting on missed execution for schedule %s: %s",
                        schedule.getScheduleId(), error.getMessage());
                metrics.recordMissedExecution(schedule.getScheduleId());
                yield updateNextExecution(updated);
            }
        };
    }

    private void handleExecutionSuccess(ScheduleExecution execution, String runId) {
        ScheduleExecution completed = execution.toBuilder()
                .status(ExecutionStatus.COMPLETED)
                .completedAt(Instant.now())
                .runId(runId)
                .build();

        activeExecutions.put(execution.getExecutionId(), completed);

        // Reset circuit breaker on success
        CircuitBreaker breaker = circuitBreakers.get(execution.getScheduleId());
        if (breaker != null) {
            breaker.recordSuccess();
        }

        metrics.recordScheduleSuccess(execution.getScheduleId());

        LOG.infof("Schedule execution completed: %s -> run %s",
                execution.getExecutionId(), runId);

        // Persist execution record
        scheduleStore.saveExecution(completed).subscribe().with(
                saved -> LOG.debug("Execution record saved"),
                error -> LOG.error("Failed to save execution record", error));
    }

    private void handleExecutionFailure(ScheduleExecution execution, Throwable error) {
        ScheduleExecution failed = execution.toBuilder()
                .status(ExecutionStatus.FAILED)
                .completedAt(Instant.now())
                .errorMessage(error.getMessage())
                .build();

        activeExecutions.put(execution.getExecutionId(), failed);

        // Record circuit breaker failure
        CircuitBreaker breaker = getCircuitBreaker(execution.getScheduleId());
        breaker.recordFailure();

        metrics.recordScheduleFailure(execution.getScheduleId());

        LOG.errorf(error, "Schedule execution failed: %s",
                execution.getExecutionId());

        // Persist execution record
        scheduleStore.saveExecution(failed).subscribe().with(
                saved -> LOG.debug("Execution record saved"),
                err -> LOG.error("Failed to save execution record", err));
    }

    private CircuitBreaker getCircuitBreaker(String scheduleId) {
        return circuitBreakers.computeIfAbsent(
                scheduleId,
                id -> new CircuitBreaker(3, 300000) // 3 failures, 5 min timeout
        );
    }

    // ========================================================================
    // Public API
    // ========================================================================

    /**
     * Create a new workflow schedule.
     */
    public Uni<WorkflowSchedule> createSchedule(ScheduleRequest request) {
        LOG.infof("Creating schedule for workflow %s", request.workflowId());

        // Validate cron expression if provided
        if (request.scheduleType() == ScheduleType.CRON) {
            validateCronExpression(request.cronExpression());
        }

        Instant firstExecution = calculateFirstExecution(request);

        WorkflowSchedule schedule = WorkflowSchedule.builder()
                .scheduleId(UUID.randomUUID().toString())
                .workflowId(request.workflowId())
                .workflowVersion(request.workflowVersion())
                .tenantId(request.tenantId())
                .scheduleType(request.scheduleType())
                .cronExpression(request.cronExpression())
                .interval(request.interval())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .timezone(request.timezone())
                .hour(request.hour())
                .minute(request.minute())
                .inputs(request.inputs())
                .missedExecutionStrategy(
                        request.missedExecutionStrategy() != null
                                ? request.missedExecutionStrategy()
                                : MissedExecutionStrategy.SKIP)
                .enabled(true)
                .createdAt(Instant.now())
                .createdBy(request.createdBy())
                .nextExecutionAt(firstExecution)
                .consecutiveFailures(0)
                .executionCount(0)
                .build();

        return scheduleStore.save(schedule)
                .onItem().invoke(saved -> LOG.infof("Created schedule %s, first execution: %s",
                        saved.getScheduleId(), firstExecution));
    }

    private Instant calculateFirstExecution(ScheduleRequest request) {
        if (request.startDate() != null && request.startDate().isAfter(Instant.now())) {
            return request.startDate();
        }

        // Calculate based on type
        return switch (request.scheduleType()) {
            case CRON -> calculateCronNext(
                    request.cronExpression(),
                    ZoneId.of(request.timezone()));
            case INTERVAL -> Instant.now().plusMillis(request.interval());
            case CALENDAR -> calculateCalendarNext(
                    request.hour(),
                    request.minute(),
                    ZoneId.of(request.timezone()));
            case ONE_TIME -> request.startDate() != null
                    ? request.startDate()
                    : Instant.now();
        };
    }

    private void validateCronExpression(String cronExpression) {
        try {
            new org.quartz.CronExpression(cronExpression);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid cron expression: " + cronExpression, e);
        }
    }

    /**
     * Update an existing schedule.
     */
    public Uni<WorkflowSchedule> updateSchedule(
            String scheduleId,
            ScheduleUpdateRequest request) {

        return scheduleStore.findById(scheduleId)
                .onItem().ifNull().failWith(
                        () -> new IllegalArgumentException("Schedule not found: " + scheduleId))
                .onItem().transform(existing -> {
                    WorkflowSchedule.Builder builder = existing.toBuilder();

                    if (request.cronExpression() != null) {
                        validateCronExpression(request.cronExpression());
                        builder.cronExpression(request.cronExpression());
                    }
                    if (request.interval() != null) {
                        builder.interval(request.interval());
                    }
                    if (request.inputs() != null) {
                        builder.inputs(request.inputs());
                    }
                    if (request.enabled() != null) {
                        builder.enabled(request.enabled());
                    }

                    return builder.build();
                })
                .onItem().transformToUni(updated -> scheduleStore.save(updated));
    }

    /**
     * Delete a schedule.
     */
    public Uni<Void> deleteSchedule(String scheduleId) {
        return scheduleStore.delete(scheduleId);
    }

    /**
     * Get schedule execution history.
     */
    public Uni<List<ScheduleExecution>> getExecutionHistory(
            String scheduleId,
            int limit) {

        return scheduleStore.findExecutions(scheduleId, limit);
    }

    /**
     * Get a schedule by ID.
     */
    public Uni<WorkflowSchedule> getSchedule(String scheduleId) {
        return scheduleStore.findById(scheduleId);
    }

    /**
     * List schedules with filtering.
     */
    public Uni<List<WorkflowSchedule>> listSchedules(String tenantId, String workflowId, Boolean enabled) {
        // Simple in-memory filtering for now
        // If tenantId is provided, start with tenant search, else all?
        // ScheduleStore doesn't expose "findAll".
        // Assuming findByTenantId is best starting point if tenantId available.
        // If not, maybe findByWorkflowId.

        Uni<List<WorkflowSchedule>> baseQuery;
        if (tenantId != null && !tenantId.isEmpty()) {
            baseQuery = scheduleStore.findByTenantId(tenantId);
        } else if (workflowId != null && !workflowId.isEmpty()) {
            baseQuery = scheduleStore.findByWorkflowId(workflowId);
        } else {
            // No easy way to list all from current store API.
            // Return empty list or fail?
            // For now, return empty if no filter provided.
            return Uni.createFrom().item(Collections.emptyList());
        }

        return baseQuery.map(list -> list.stream()
                .filter(s -> workflowId == null || workflowId.isEmpty() || s.getWorkflowId().equals(workflowId))
                .filter(s -> enabled == null || s.isEnabled() == enabled)
                .collect(java.util.stream.Collectors.toList()));
    }
}