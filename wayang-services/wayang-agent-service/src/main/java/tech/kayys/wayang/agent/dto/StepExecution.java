package tech.kayys.wayang.agent.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record StepExecution(
        String nodeId,
        String status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Map<String, Object> inputs,
        Map<String, Object> outputs,
        String errorMessage) {
}