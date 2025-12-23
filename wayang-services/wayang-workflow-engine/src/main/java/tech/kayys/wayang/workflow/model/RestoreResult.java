package tech.kayys.wayang.workflow.model;

import java.time.Instant;

import lombok.Data;
import lombok.Builder;

@Data
@Builder(toBuilder = true)
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class RestoreResult {
    private String backupId;
    private Instant targetTimestamp;
    private long replayedEvents;
    private RestoreStatus status;
    private long restoredItemCount;
    private Instant restoreTime;
    private String errorMessage;

    public static RestoreResult success(String backupId, long restoredItemCount) {
        return RestoreResult.builder()
                .backupId(backupId)
                .status(RestoreStatus.COMPLETED)
                .restoredItemCount(restoredItemCount)
                .restoreTime(Instant.now())
                .build();
    }

    public static RestoreResult failed(String backupId, String errorMessage) {
        return RestoreResult.builder()
                .backupId(backupId)
                .status(RestoreStatus.FAILED)
                .errorMessage(errorMessage)
                .restoreTime(Instant.now())
                .build();
    }

    public enum RestoreStatus {
        COMPLETED,
        FAILED
    }
}
