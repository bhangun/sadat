package tech.kayys.wayang.workflow.scheduler.dto;

import java.time.Instant;
import java.util.Map;

public record ScheduleRequest(
        String workflowId,
        String workflowVersion,
        String tenantId,
        ScheduleType scheduleType,
        String cronExpression,
        Long interval,
        Instant startDate,
        Instant endDate,
        String timezone,
        int hour,
        int minute,
        Map<String, Object> inputs,
        MissedExecutionStrategy missedExecutionStrategy,
        String createdBy) {
}
