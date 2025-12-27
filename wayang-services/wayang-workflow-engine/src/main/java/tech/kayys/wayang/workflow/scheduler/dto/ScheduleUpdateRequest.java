package tech.kayys.wayang.workflow.scheduler.dto;

import java.util.Map;

public record ScheduleUpdateRequest(
        String cronExpression,
        Long interval,
        Map<String, Object> inputs,
        Boolean enabled) {
}
