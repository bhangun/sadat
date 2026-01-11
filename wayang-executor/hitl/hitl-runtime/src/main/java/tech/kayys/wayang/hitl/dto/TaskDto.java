package tech.kayys.wayang.hitl.dto;

import tech.kayys.wayang.hitl.domain.HumanTaskStatus;
import tech.kayys.wayang.hitl.domain.TaskOutcome;

import java.time.Instant;
import java.util.List;
import java.util.Map;

record TaskDto(
    String taskId,
    String workflowRunId,
    String nodeId,
    String taskType,
    String title,
    String description,
    int priority,
    String status,
    String assigneeType,
    String assigneeIdentifier,
    String assignedBy,
    Instant createdAt,
    Instant claimedAt,
    Instant completedAt,
    Instant dueDate,
    String outcome,
    String completedBy,
    String comments,
    Map<String, Object> formData,
    boolean escalated,
    String escalatedTo
) {}