package tech.kayys.silat.model;

import java.time.Instant;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RunResponse {
    private String runId;
    private String workflowId;
    private String workflowVersion;
    private String status;
    private String phase;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private Long durationMs;
    private Integer nodesExecuted;
    private Integer nodesTotal;
    private Integer attemptNumber;
    private Integer maxAttempts;
    private String errorMessage;
    private Map<String, Object> outputs;
}
