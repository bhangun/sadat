package tech.kayys.wayang.automation.dto;

import java.time.Instant;
import java.util.Map;

public record PendingApproval(
        String taskId,
        String processId,
        Map<String, Object> details,
        Instant createdAt,
        Instant dueAt) {
}