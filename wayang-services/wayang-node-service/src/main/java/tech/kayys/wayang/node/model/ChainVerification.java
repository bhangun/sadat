package tech.kayys.wayang.workflow.model;

import java.util.List;

/**
 * Chain verification result.
 */
@lombok.Data
@lombok.Builder
class ChainVerification {
    private String runId;
    private boolean valid;
    private List<String> violations;
    private int totalEvents;
}
