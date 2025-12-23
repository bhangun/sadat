package tech.kayys.wayang.workflow.api.dto;

import java.time.Instant;
import lombok.Data;
import lombok.Builder;
import tech.kayys.wayang.workflow.model.RestoreResult;

@Data
@Builder
public class RestoreResponse {
    private String backupId;
    private Instant targetTimestamp;
    private long replayedEvents;
    private String status;
    private long restoredItemCount;
    private Instant restoreTime;
    private String errorMessage;

    public static RestoreResponse fromModel(RestoreResult result) {
        return RestoreResponse.builder()
                .backupId(result.getBackupId())
                .targetTimestamp(result.getTargetTimestamp())
                .replayedEvents(result.getReplayedEvents())
                .status(result.getStatus() != null ? result.getStatus().name() : null)
                .restoredItemCount(result.getRestoredItemCount())
                .restoreTime(result.getRestoreTime())
                .errorMessage(result.getErrorMessage())
                .build();
    }
}
