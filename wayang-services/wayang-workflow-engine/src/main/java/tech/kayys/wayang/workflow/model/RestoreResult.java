package tech.kayys.wayang.workflow.model;

import java.time.Instant;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class RestoreResult {
    private String backupId;
    private Instant targetTimestamp;
    private long replayedEvents;
}
