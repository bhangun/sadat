package tech.kayys.wayang.workflow.api.dto;

import java.time.Instant;
import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class CleanupResultResponse {
    private int deletedCount;
    private long freedSpace;
    private Instant cleanedAt;
}
