package tech.kayys.wayang.workflow.scheduler.dto;

import java.time.Instant;

public record ScheduleResponse(
        String scheduleId,
        String workflowId,
        String status,
        Instant nextExecutionAt,
        int executionCount) {
}
