package tech.kayys.wayang.workflow.model;

import java.time.Instant;
import java.util.Map;

/**
 * Compliance report.
 */
@lombok.Data
class ComplianceReport {
    private String runId;
    private ReportType reportType;
    private Instant generatedAt;
    private long totalEvents;
    private long nodeExecutions;
    private long humanInterventions;
    private long errorCount;
    private Map<EventType, Long> eventBreakdown;
}
