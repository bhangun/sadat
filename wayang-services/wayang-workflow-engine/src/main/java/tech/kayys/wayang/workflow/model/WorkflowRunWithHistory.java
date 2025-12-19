package tech.kayys.wayang.workflow.model;

import java.util.List;

import tech.kayys.wayang.workflow.api.model.WorkflowEvent;
import tech.kayys.wayang.workflow.domain.WorkflowRun;

/**
 * Workflow run with event history
 */
record WorkflowRunWithHistory(
        WorkflowRun run,
        List<WorkflowEvent> events) {
    public int eventCount() {
        return events.size();
    }
}
