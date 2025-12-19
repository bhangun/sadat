package tech.kayys.wayang.workflow.model;

import java.util.List;

import tech.kayys.wayang.workflow.domain.WorkflowRun;

/**
 * Workflow run details with events.
 */
@lombok.Data
@lombok.Builder
public class WorkflowRunDetails {
    private WorkflowRun run;
    private List<Object> events; // ProvenanceEvent
}
