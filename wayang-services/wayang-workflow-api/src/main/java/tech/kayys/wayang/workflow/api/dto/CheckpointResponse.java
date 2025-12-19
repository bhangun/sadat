package tech.kayys.wayang.workflow.api.dto;

import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckpointResponse {
    private String checkpointId;
    private String runId;
    private Integer sequenceNumber;
    private String status;
    private Integer nodesExecuted;
    private Instant createdAt;
}
