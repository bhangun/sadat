package tech.kayys.wayang.workflow.model;

import java.time.Instant;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Human task snapshot
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class HumanTaskSnapshot {
    private String taskId;
    private String nodeId;
    private String status;
    private String assignedTo;
    private Instant createdAt;
    private Instant dueDate;
    private Map<String, Object> formData;
}
