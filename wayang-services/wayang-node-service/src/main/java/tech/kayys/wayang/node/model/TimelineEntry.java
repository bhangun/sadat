package tech.kayys.wayang.workflow.model;

import java.time.Instant;

/**
 * Timeline entry for visualization.
 */
@lombok.Data
@lombok.Builder
public class TimelineEntry {
    private Instant timestamp;
    private EventType eventType;
    private String nodeId;
    private String summary;
}
