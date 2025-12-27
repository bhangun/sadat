package tech.kayys.wayang.workflow.scheduler.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * ScheduleMetrics: Metrics collection for workflow scheduling.
 */
@ApplicationScoped
public class ScheduleMetrics {

    @Inject
    MeterRegistry registry;

    public void recordScheduleExecution(String scheduleId) {
        Counter.builder("schedule.execution.total")
                .tag("schedule_id", scheduleId)
                .register(registry)
                .increment();
    }

    public void recordScheduleSuccess(String scheduleId) {
        Counter.builder("schedule.execution.success")
                .tag("schedule_id", scheduleId)
                .register(registry)
                .increment();
    }

    public void recordScheduleFailure(String scheduleId) {
        Counter.builder("schedule.execution.failure")
                .tag("schedule_id", scheduleId)
                .register(registry)
                .increment();
    }

    public void recordMissedExecution(String scheduleId) {
        Counter.builder("schedule.execution.missed")
                .tag("schedule_id", scheduleId)
                .register(registry)
                .increment();
    }
}