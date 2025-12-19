package tech.kayys.wayang.agent.dto;

import java.time.LocalDateTime;
import java.util.List;

public record WorkflowExecution(
        String id,
        String workflowId,
        String tenantId,
        String status,
        List<StepExecution> steps,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Object result) {
}