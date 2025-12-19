package tech.kayys.wayang.sdk.dto.htil;

import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;



/**
 * Human task response
 */
public record HumanTaskResponse(
    String taskId,
    String runId,
    String correlationKey,
    TaskType taskType,
    TaskPriority priority,
    TaskStatus status,
    String assignedTo,
    Instant createdAt,
    Instant dueAt,
    Map<String, Object> context,
    List<TaskAction> availableActions
) {}
