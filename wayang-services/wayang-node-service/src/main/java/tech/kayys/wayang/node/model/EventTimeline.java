package tech.kayys.wayang.workflow.model;

/**
 * Event timeline container.
 */
@lombok.Data
@lombok.Builder
class EventTimeline {
    private String runId;
    private List<TimelineEntry> entries;
}
